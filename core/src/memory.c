#include <memory.h>
#include <process.h>
#include <pthread.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

struct mm_segment_t mem, sys_mem;
struct mem_list *mem_head, *sys_mem_head;
pthread_mutex_t mem_lock;

void init_mem()
{
    pthread_mutex_init(&mem_lock, NULL);

    // init total memory
    mem.st = (char *)malloc(MEM_BYTES);
    mem.ed = mem.st + MEM_BYTES;
    memset(mem.st, 0, MEM_BYTES);

    // init system memory
    sys_mem.st = mem.st;
    sys_mem.ed = sys_mem.st + SYS_MEM_BYTES;

    // build memory linked list
    mem_head = (struct mem_list *)malloc(sizeof(struct mem_list));
    mem_head->pid = -1;
    mem_head->nex = mem_head->prev = NULL;
    mem_head->mem.st = sys_mem.ed;
    mem_head->mem.ed = mem.ed;

    sys_mem_head = (struct mem_list *)malloc(sizeof(struct mem_list));
    sys_mem_head->pid = -1;
    sys_mem_head->nex = sys_mem_head->prev = NULL;
    sys_mem_head->mem.st = sys_mem.st + sizeof(struct sys_info_t);
    sys_mem_head->mem.ed = sys_mem.ed;
}

struct mm_segment_t get_sys_mem()
{
    return sys_mem;
}

struct mem_list *get_mem_list()
{
    return mem_head;
}

struct mem_list *get_sys_mem_list()
{
    return sys_mem_head;
}

// should be thread safe
void *_malloc(pid_t pid, size_t __size)
{
    if (__size <= 0)
        return NULL;
    pthread_mutex_lock(&mem_lock);
    struct mem_list *ptr;
    if (pid == 0)
        ptr = sys_mem_head;
    else
        ptr = mem_head;
    while (ptr != NULL)
    {
        size_t len = ptr->mem.ed - ptr->mem.st;
        if (len < __size || ptr->pid != -1)
        {
            ptr = ptr->nex;
            continue;
        }
        if (len > __size)
        {
            char *p = ptr->mem.st + __size;
            struct mem_list *mem_node = (struct mem_list *)malloc(sizeof(struct mem_list));
            mem_node->pid = -1;
            mem_node->mem.st = p;
            mem_node->mem.ed = ptr->mem.ed;
            ptr->mem.ed = p;
            mem_node->nex = ptr->nex;
            mem_node->prev = ptr;
            if (ptr->nex != NULL)
                ptr->nex->prev = mem_node;
            ptr->nex = mem_node;
        }
        ptr->pid = pid;
        pthread_mutex_unlock(&mem_lock);
        return ptr->mem.st;
    }
    pthread_mutex_unlock(&mem_lock);
    return NULL;
}

// should be thread safe
void _free(void *__ptr, int sys)
{
    pthread_mutex_lock(&mem_lock);
    struct mem_list *ptr;
    if (sys == 1)
        ptr = sys_mem_head;
    else
        ptr = mem_head;
    while (ptr->mem.st != (char *)__ptr)
        ptr = ptr->nex;
    if (ptr == NULL)
    {
        pthread_mutex_unlock(&mem_lock);
        return;
    }
    struct mem_list *last = ptr->prev, *next = ptr->nex;
    ptr->pid = -1;
    // try to merge free block
    if (last != NULL && last->pid == -1)
    {
        last->mem.ed = ptr->mem.ed;
        last->nex = ptr->nex;
        if (next != NULL)
            next->prev = last;
        free(ptr);
        ptr = last;
    }
    if (next != NULL && next->pid == -1)
    {
        ptr->mem.ed = next->mem.ed;
        ptr->nex = next->nex;
        if (next->nex != NULL)
            next->nex->prev = ptr;
        free(next);
    }
    pthread_mutex_unlock(&mem_lock);
}

void _sys_free(void *__ptr)
{
    _free(__ptr, 1);
}

void _usr_free(void *__ptr)
{
    _free(__ptr, 0);
}