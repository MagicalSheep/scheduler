#include <interface.h>
#include <iodevice.h>
#include <memory.h>
#include <processor.h>
#include <pthread.h>
#include <signal.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/time.h>
#include <unistd.h>
#define INTERVAL_SECONDS 1
#define CORES_COUNT 1
#define PROC_COUNT 10

pthread_t mtid, iotid;
struct sys_info_t *sys_ptr = NULL;

void timer_trap()
{
    if (mtid != pthread_self())
        return;
    if (sys_ptr == NULL)
        return;
    // printf("[%ld] Trap\n", pthread_self());
    for (int i = 0; i < CORES_COUNT; i++)
        pthread_kill(sys_ptr->processors[i].tid, SIGALRM);
}

void power_trap()
{
    printf("\nSystem closed\n");
    exit(EXIT_SUCCESS);
}

int init()
{
    // init memory
    init_mem();
    struct mm_segment_t sys_mem = get_sys_mem();
    sys_ptr = (struct sys_info_t *)sys_mem.st;
    sys_ptr->cpu_cnt = CORES_COUNT;
    sys_ptr->proc_cnt = PROC_COUNT;
    sys_ptr->processors = (struct processor *)_malloc(0, CORES_COUNT * sizeof(struct processor));

    // init io device
    if (pthread_create(&iotid, NULL, (void *)io_work, NULL) < 0)
        return -1;

    // init processor
    for (int i = 0; i < CORES_COUNT; i++)
    {
        if (pthread_mutex_init(&sys_ptr->processors[i].lock, NULL) != 0)
            return -1;
        sys_ptr->processors[i].sys_mem = sys_mem;
        sys_ptr->processors[i].cores_id = i;
        if (pthread_create(&sys_ptr->processors[i].tid, NULL, (void *)work, (void *)(&sys_ptr->processors[i])) < 0)
            return -1;
    }
    pthread_t tid;
    pthread_create(&tid, NULL, (void *)init_interface, NULL);
    pthread_detach(tid);
    return 0;
}

int main(int argc, char const *argv[])
{
    mtid = pthread_self();

    // init hardware timer
    signal(SIGALRM, timer_trap);
    signal(SIGINT, power_trap);
    struct itimerval v;
    v.it_interval.tv_sec = v.it_value.tv_sec = INTERVAL_SECONDS;
    v.it_interval.tv_usec = v.it_value.tv_usec = 0;
    if (setitimer(ITIMER_REAL, &v, NULL) < 0)
        goto error;

    // init system
    if (init() < 0)
        goto error;
    while (1)
        ;

success:
    exit(EXIT_SUCCESS);
error:
    perror("Error");
    exit(EXIT_FAILURE);
}