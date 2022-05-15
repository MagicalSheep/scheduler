#ifndef _MEMORY_H_
#define _MEMORY_H_
#define MEM_BYTES 64000000
#define SYS_MEM_BYTES 16000000
#include <sys/types.h>

struct mm_segment_t
{
    char *st, *ed;
};

/**
 * Description of program memory structure
 * and running state (cp register)
 *
 * It's address offset
 */
struct mm_struct
{
    unsigned int start_code, end_code, cp;
    unsigned int start_data, end_data;
};

struct mem_list
{
    pid_t pid; // if -1 then available
    struct mm_segment_t mem;
    struct mem_list *nex, *prev;
};

/**
 * Initialize memory and build the memory linked list
 *
 * Memory is divided into system part and user part
 */
void init_mem();

struct mm_segment_t get_sys_mem();

struct mem_list *get_mem_list();

struct mem_list *get_sys_mem_list();

/**
 * It will return the kernel space memory pointer if pid = 0
 */
void *_malloc(pid_t pid, size_t __size);

void _sys_free(void *__ptr);

void _usr_free(void *__ptr);

#endif