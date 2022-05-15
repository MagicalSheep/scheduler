#include <iodevice.h>
#include <pthread.h>
#include <signal.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>

struct io_op_head iorq;
pthread_mutex_t rq_mutex;

int push_back_io_task(struct io_op op)
{
    // note: we can assume that the function run quickly
    // so just make CPU block here to wait for submiting an I/O request
    pthread_mutex_lock(&rq_mutex);

    struct io_op_list *ptr = (struct io_op_list *)malloc(sizeof(struct io_op_list));
    ptr->val = op;
    ptr->nex = NULL;
    if (iorq.size == 0)
        iorq.head = iorq.tail = ptr;
    else
        iorq.tail->nex = ptr, iorq.tail = ptr;
    ++iorq.size;

    if (pthread_mutex_unlock(&rq_mutex) != 0)
        return -1; // make CPU crash

    return 0;
}

void pop_front_io_task(struct io_op *op)
{
    struct io_op_list *ptr = iorq.head;
    iorq.head = ptr->nex;
    *op = ptr->val;
    --(iorq.size);
    free(ptr);
}

void io_work()
{
    // init io device
    struct sys_info_t *sys_ptr = (struct sys_info_t *)(get_sys_mem().st);
    iorq.head = iorq.tail = NULL, iorq.size = 0;
    if (pthread_mutex_init(&rq_mutex, NULL) != 0)
        goto error;
    // work
    struct task_struct_list *ptr;
    struct task_struct *task;
    struct io_op op;
    while (1)
    {
        if (iorq.size == 0)
            continue;
        pop_front_io_task(&op);
        sleep(op.times);

        // get task memory address (maybe in outer memory)
        task = NULL;
        ptr = op.processor->iorq.head;
        while (ptr != NULL)
        {
            if (ptr->val->pid == op.pid)
            {
                task = ptr->val;
                break;
            }
            ptr = ptr->nex;
        }
        if (task != NULL)
            goto loop_lock;
        ptr = sys_ptr->suspend_queue.head;
        while (ptr != NULL)
        {
            if (ptr->val->pid == op.pid)
            {
                task = ptr->val;
                break;
            }
            ptr = ptr->nex;
        }
        if (task == NULL)
            continue;
        // when I/O operation completed, only two states may happen
    loop_lock:
        if (task->state == TASK_INTERRUPTIBLE)
            task->state = TASK_READY;
        else if (task->state == TASK_IO_SUSPEND)
            task->state = TASK_SUSPEND;
        else                // suspend state(I/O completed before middle schedule)
            goto loop_lock; // self lock until middle schedule completed
        // we can assume that middle schedule will completed in a short time,
        // so this loop may not run long time.
        pthread_kill(op.processor->tid, SIGALRM);
    }
error:
    printf("Error: I/O device crash\n");
}