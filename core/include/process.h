#ifndef _PROCESS_H_
#define _PROCESS_H_
#include "memory.h"
#include <sys/types.h>
#define TASK_RUNNING 0
#define TASK_READY 1
#define TASK_INTERRUPTIBLE 2
#define TASK_STOPPED 3
#define TASK_SUSPEND 4
#define TASK_IO_SUSPEND 5
#define JOB_RUNNING 0
#define JOB_READY 1
#define JOB_COMPLETED 2
#define JOB_FAILED 3
#define MAX_PRIOR 60
#include "command.h"
#include <sys/types.h>

struct job_struct
{
    int id;
    size_t siz;
    int prior, cmd_cnt, status;
    struct cmd_struct *cmd;
    char *msg;
};

struct job_struct_list
{
    struct job_struct *val;
    struct job_struct_list *nex;
};

struct job_struct_head
{
    unsigned long size;
    struct job_struct_list *head, *tail;
};

struct task_struct
{
    volatile long state;
    pid_t pid;
    struct mm_segment_t addr_limit;
    struct mm_struct mm;
    int prio, processor;
    struct job_struct *job;
};

struct task_struct_list
{
    struct task_struct *val;
    struct task_struct_list *nex, *prev;
};

struct task_struct_head
{
    unsigned long size;
    struct task_struct_list *head, *tail;
};

/**
 * System fix memory structure
 */
struct sys_info_t
{
    int cpu_cnt, pid_cnt, proc_cnt, run_proc_cnt, job_cnt;
    struct job_struct_head pool_queue, job_res;
    struct task_struct_head suspend_queue;
    struct processor *processors;
};

#endif