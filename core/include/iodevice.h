#ifndef _IO_DEVICE_H_
#define _IO_DEVICE_H_
#include "process.h"
#include "processor.h"

/**
 * Description of an I/O operation
 */
struct io_op
{
    pid_t pid;
    unsigned long times;
    struct processor *processor;
};

struct io_op_list
{
    struct io_op val;
    struct io_op_list *nex;
};

struct io_op_head
{
    int size;
    struct io_op_list *head, *tail;
};

void io_work();

int push_back_io_task(struct io_op op);

#endif