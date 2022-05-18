#ifndef _PROCESSOR_H_
#define _PROCESSOR_H_
#include "process.h"
#include <pthread.h>
#include <stdlib.h>
#include <string.h>
// sus_q: suspend queue head pointer, n_ptr: list node pointer
#define INSERT_SUSPEND_QUEUE(sus_q, n_ptr)                                                   \
    do                                                                                       \
    {                                                                                        \
        struct task_struct *task = (struct task_struct *)malloc(sizeof(struct task_struct)); \
        memcpy(task, n_ptr->val, sizeof(struct task_struct));                                \
        size_t len = n_ptr->val->addr_limit.ed - n_ptr->val->addr_limit.st;                  \
        char *space = (char *)malloc(len);                                                   \
        memcpy(space, n_ptr->val->addr_limit.st, len);                                       \
        task->addr_limit.st = space;                                                         \
        task->addr_limit.ed = space + len;                                                   \
        _usr_free(n_ptr->val->addr_limit.st);                                                \
        _sys_free(n_ptr->val);                                                               \
        n_ptr->val = task;                                                                   \
        if (sus_q->size == 0)                                                                \
            sus_q->head = sus_q->tail = n_ptr;                                               \
        else                                                                                 \
            sus_q->tail->nex = n_ptr, n_ptr->prev = sus_q->tail, sus_q->tail = n_ptr;        \
        ++(sus_q->size);                                                                     \
    } while (0)
// rq: queue head pointer, n_ptr: list node pointer
#define REMOVE_LIST_NODE(rq, n_ptr)         \
    do                                      \
    {                                       \
        if (n_ptr->prev == NULL)            \
            rq->head = n_ptr->nex;          \
        else                                \
            n_ptr->prev->nex = n_ptr->nex;  \
        if (n_ptr->nex == NULL)             \
            rq->tail = n_ptr->prev;         \
        else                                \
            n_ptr->nex->prev = n_ptr->prev; \
        n_ptr->nex = n_ptr->prev = NULL;    \
        --(rq->size);                       \
    } while (0)

struct processor
{
    struct mm_segment_t sys_mem;
    unsigned int cores_id, tasks_cnt;
    pthread_t tid;
    pthread_mutex_t lock;
    struct task_struct_head rq[MAX_PRIOR + 1], iorq;
    int mid_schedule_status;
    struct task_struct *cur;
};

/**
 * should be thread safe
 */
void push_job_res(struct job_struct *job);

/**
 * Middle scheduler function entry
 *
 * This function should be thread safe
 */
int do_mid_schedule(struct processor *p);

/**
 * Long scheduler funtion entry
 *
 * Since there is only one long scheduler instance in the system,
 * this function should be thread safe
 */
int do_long_schedule(int enter_state);

/**
 * Short scheduler function entry.
 *
 * Check whether there is an I/O process ready
 * and then switch a process from the priority queue to exec.
 */
int do_schedule(struct processor *p, struct task_struct **task);

/**
 * Executor. It's the function where CPU actually work.
 * It recovers from the cut point and execute the remain codes.
 *
 * When an I/O operation happened, the function will return to short scheduler immediately
 * and asks for an I/O request.
 *
 * Executing could be interrupted by the timer trap.
 * If interrupted, executor will save the running states to the PCB structure
 * and return to short scheduler immediately.
 *
 * @return If task completed return 0, else -1.
 * Specially, an I/O request will cause returning a positive integer
 * which specifies the I/O times (since the project is just a simulation).
 */
int exec(struct task_struct *task, struct mm_segment_t sys_mem);

/**
 *  CPU work
 */
void work(void *args);

/**
 * set thread mutex before using this function
 * we can assume that the function run quickly
 */
void push_back_task(struct task_struct_head *list, struct task_struct *val);

void pop_front_task(struct task_struct_head *list, struct task_struct **val);

#endif