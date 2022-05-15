#include <command.h>
#include <interface.h>
#include <iodevice.h>
#include <processor.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

void push_back_task(struct task_struct_head *list, struct task_struct *val)
{
    struct task_struct_list *ptr = (struct task_struct_list *)_malloc(0, sizeof(struct task_struct_list));
    ptr->val = val;
    ptr->prev = list->tail;
    ptr->nex = NULL;
    if (list->size == 0)
        list->head = list->tail = ptr;
    else
        list->tail->nex = ptr, list->tail = ptr;
    ++(list->size);
}

void pop_front_task(struct task_struct_head *list, struct task_struct **val)
{
    struct task_struct_list *ptr = list->head;
    list->head = ptr->nex;
    if (list->head == NULL)
        list->tail = NULL;
    if (list->head != NULL)
        list->head->prev = NULL;
    *val = ptr->val;
    --(list->size);
    _sys_free(ptr);
}

pthread_mutex_t job_res_mutex;

void push_job_res(struct job_struct *job)
{
    pthread_mutex_lock(&job_res_mutex);

    struct sys_info_t *sys_ptr = (struct sys_info_t *)(get_sys_mem().st);
    struct job_struct_list *job_node = (struct job_struct_list *)_malloc(0, sizeof(struct job_struct_list));
    job_node->nex = NULL;
    job_node->val = job;
    struct job_struct_head *list = &(sys_ptr->job_res);
    if (list->size == 0)
        list->head = list->tail = job_node;
    else
        list->tail->nex = job_node, list->tail = job_node;
    ++(list->size);

    pthread_mutex_unlock(&job_res_mutex);
}

pthread_mutex_t mid_scheduler_mutex;

int do_mid_schedule(struct processor *p)
{
    if (pthread_mutex_trylock(&mid_scheduler_mutex) != 0)
        return 0;

    p->mid_schedule_status = 0;
    struct sys_info_t *sys_ptr = (struct sys_info_t *)(get_sys_mem().st);
    struct task_struct_head *sus_q = &(sys_ptr->suspend_queue);
    struct task_struct_list *sp = sus_q->head, *tmp;

    while (sp != NULL)
    {
        tmp = sp->nex;
        if (sp->val->state == TASK_READY || sp->val->state == TASK_INTERRUPTIBLE)
        {
            REMOVE_LIST_NODE(sus_q, sp);

            // recover from disk
            struct task_struct *task = (struct task_struct *)_malloc(0, sizeof(struct task_struct));
            memcpy(task, sp->val, sizeof(struct task_struct));
            size_t siz = sp->val->addr_limit.ed - sp->val->addr_limit.st;
            task->addr_limit.st = (char *)_malloc(sp->val->pid, siz);
            task->addr_limit.ed = task->addr_limit.st + siz;
            // free disk space
            free(sp->val);
            _sys_free(sp);

            printf("[CPU%u] User unsuspend the process %d\n", p->cores_id, task->pid);

            // insert node to queue
            if (task->state == TASK_READY)
                push_back_task(&(p->rq[task->prio]), task);
            else
                push_back_task(&(p->iorq), task);
            ++(((struct sys_info_t *)(get_sys_mem().st))->run_proc_cnt);
        }
        sp = tmp;
    }

    for (int i = MAX_PRIOR; i >= 0; i--)
    {
        if (p->rq[i].size == 0)
            continue;
        struct task_struct_list *ptr = p->rq[i].head;
        while (ptr != NULL)
        {
            tmp = ptr->nex;
            if (ptr->val->state == TASK_SUSPEND)
            {
                REMOVE_LIST_NODE((&(p->rq[i])), ptr);
                INSERT_SUSPEND_QUEUE(sus_q, ptr);
                --(((struct sys_info_t *)(get_sys_mem().st))->run_proc_cnt);
            }
            ptr = tmp;
        }
    }

    sp = p->iorq.head;
    while (sp != NULL)
    {
        tmp = sp->nex;
        if (sp->val->state == TASK_SUSPEND)
        {
            sp->val->state = TASK_IO_SUSPEND;
            REMOVE_LIST_NODE((&(p->iorq)), sp);
            INSERT_SUSPEND_QUEUE(sus_q, sp);
            --(((struct sys_info_t *)(get_sys_mem().st))->run_proc_cnt);
        }
        sp = tmp;
    }

    if (pthread_mutex_unlock(&mid_scheduler_mutex) != 0)
        return -1;

    return 0;
}

pthread_mutex_t long_scheduler_mutex;

int do_long_schedule(int enter_state)
{
    if (enter_state == JOB_COMPLETED)
    {
        // if enter from finshing a job, just try to do a long schedule
        if (pthread_mutex_trylock(&long_scheduler_mutex) != 0)
            return 0;
    }
    else
    {
        // if it is a new job, should block here to make sure that
        // the long scheduler work at least once
        pthread_mutex_lock(&long_scheduler_mutex);
    }

    struct sys_info_t *sys_ptr = (struct sys_info_t *)(get_sys_mem().st);
    struct job_struct_head *list = &(sys_ptr->pool_queue);
    while (list->size > 0 && sys_ptr->run_proc_cnt < sys_ptr->proc_cnt)
    {
        struct job_struct_list *ptr = list->head;
        struct job_struct *job = ptr->val;
        list->head = ptr->nex;
        --(list->size);
        _sys_free(ptr);
        // if failed to create process, will generate the job result and skip running
        create_process(job);
    }

    if (pthread_mutex_unlock(&long_scheduler_mutex) != 0)
        return -1;

    return 0;
}

int do_schedule(struct processor *p, struct task_struct **task)
{
    if (pthread_mutex_lock(&p->lock) != 0)
        return -1;

    if (p->mid_schedule_status)
        do_mid_schedule(p);

    struct task_struct_list *ptr = p->iorq.head, *tmp;
    // check iorq
    while (ptr != NULL)
    {
        tmp = ptr->nex;
        if (ptr->val->state != TASK_READY)
            goto nex;
        REMOVE_LIST_NODE((&(p->iorq)), ptr);
        push_back_task(&p->rq[ptr->val->prio], ptr->val);
        _sys_free(ptr);
    nex:
        ptr = tmp;
    }
    if (pthread_mutex_unlock(&p->lock) != 0)
        return -1;

    // switch process to exec
    int i;
    for (i = MAX_PRIOR; i >= 0 && p->rq[i].size <= 0; i--)
        ;
    if (i < 0)
        return -1;
    pop_front_task(&p->rq[i], task);

    return 0;
}

int exec(struct task_struct *task, struct mm_segment_t sys_mem)
{
    // recovery
    struct cmd_struct *cp = (struct cmd_struct *)(task->addr_limit.st + task->mm.start_code + task->mm.cp);

    // exec
    printf("[PID %d] Entry address is %p\n", task->pid, cp);

    while (cp < (struct cmd_struct *)(task->addr_limit.st + task->mm.end_code))
    {
        if (cp->type == IO_CMD)
        {
            unsigned long times = cp->times;
            // make offset ahead and save it to the PCB
            task->mm.cp = ((char *)(++cp)) - task->addr_limit.st - task->mm.start_code;
            return times;
        }
        if ((cp->times = sleep(cp->times)) == 0)
            ++cp;
        else
            break;
    }
    if (cp >= (struct cmd_struct *)(task->addr_limit.st + task->mm.end_code))
        return 0;

    // save
    task->mm.cp = ((char *)cp) - task->addr_limit.st - task->mm.start_code;

    printf("[PID %d] Save process state successfully\n", task->pid);

    return -1;
}

void work(void *args)
{
    struct processor *p = (struct processor *)args;
    struct sys_info_t *sys_ptr = (struct sys_info_t *)(get_sys_mem().st);
    struct task_struct *task;
    if (pthread_mutex_init(&long_scheduler_mutex, NULL) != 0)
        goto error;
    if (pthread_mutex_init(&job_res_mutex, NULL) != 0)
        goto error;
    if (pthread_mutex_init(&mid_scheduler_mutex, NULL) != 0)
        goto error;
    while (1)
    {
        if (do_schedule(p, &task) < 0)
            continue;

        printf("[CPU%u] Execute the process %d\n", p->cores_id, task->pid);
        p->cur = task;
        int status = exec(task, p->sys_mem);

        // completed
        if (status == 0)
        {

            printf("[CPU%u] Process %d completed\n", p->cores_id, task->pid);
            if (p->iorq.head != NULL)
                p->cur = p->iorq.head->val;
            else
                p->cur = NULL;

            // save job result to the disk
            task->job->status = JOB_COMPLETED;
            task->job->msg = "Job completed";
            push_job_res(task->job);

            // clear task memory
            task->state = TASK_STOPPED;
            --(p->tasks_cnt);
            --(sys_ptr->run_proc_cnt);
            _usr_free(task->addr_limit.st);
            _sys_free(task);

            if (do_long_schedule(JOB_COMPLETED) < 0)
                goto error;

            continue;
        }
        // trap in io device
        if (status > 0)
        {
            if (task->state != TASK_SUSPEND)
                printf("[CPU%u] Stop executing the process %d because of I/O request\n", p->cores_id, task->pid);
            else
                printf("[CPU%u] User suspend the process %d\n", p->cores_id, task->pid);

            if (task->state != TASK_SUSPEND)
                task->state = TASK_INTERRUPTIBLE;
            task->prio = MAX_PRIOR; // if I/O ready, run process immediately
            push_back_task(&p->iorq, task);
            struct io_op io;
            io.times = status;
            io.processor = p;
            io.pid = task->pid;
            if (push_back_io_task(io) < 0)
                goto error;
            continue;
        }

        if (task->state != TASK_SUSPEND)
            printf("[CPU%u] Stop executing the process %d because of timer interrupt\n", p->cores_id, task->pid);
        else
            printf("[CPU%u] User suspend the process %d\n", p->cores_id, task->pid);

        if (task->state != TASK_SUSPEND)
            task->state = TASK_READY;
        --task->prio;
        if (task->prio < 0)
            task->prio = 0;

        if (pthread_mutex_lock(&p->lock) != 0)
            goto error;
        push_back_task(&p->rq[task->prio], task);
        if (pthread_mutex_unlock(&p->lock) != 0)
            goto error;
    }
error:
    printf("Error: CPU%u crash\n", p->cores_id);
}