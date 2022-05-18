#ifndef _INTERFACE_H_
#define _INTERFACE_H_
#include "command.h"
#include "process.h"
#include "processor.h"
#include <sys/types.h>

/**
 * Open a socket shell to control the system
 */
void init_interface();

/**
 * This function is the entry to create a job
 */
int create_job(size_t mem_size, int prior, struct cmd_struct *cmd_seq, int cmd_cnt);

/**
 * This function is the entry to create a process from a specific job
 */
int create_process(struct job_struct *job);

int suspend_process(int core_id);

int unsuspend_process(pid_t pid);

#endif