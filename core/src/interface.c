#include <interface.h>
#include <memory.h>
#include <netinet/in.h>
#include <netinet/ip.h>
#include <pthread.h>
#include <signal.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/socket.h>
#include <sys/types.h>
#include <unistd.h>
#define BUF_SIZE 655350

static int PORT = 8080, boss_fd;

/**
 * Parse the command from socket and execute the command
 */
void interface_work(void *args)
{
    int fd = *((int *)args);
    char buf[BUF_SIZE];
    ssize_t rret;
    if ((rret = recv(fd, buf, BUF_SIZE, 0)) <= 0)
        goto ed;
    buf[rret] = '\0';
    switch (buf[0])
    {
    case '0': // submit job
    {
        char *s = buf + 1;
        struct cmd_struct cmds[100];
        size_t mem_siz = -1;
        int cmd_cnt = 0, prior = -1;
        for (char *p = strtok(s, ","); p != NULL; p = strtok(NULL, ","))
        {
            char *tmp;
            if (mem_siz == -1)
            {
                mem_siz = strtol(p, &tmp, 10);
                continue;
            }
            if (prior == -1)
            {
                prior = strtol(p, &tmp, 10);
                continue;
            }
            cmds[cmd_cnt].type = p[0] - '0';
            ++p;
            cmds[cmd_cnt++].times = strtol(p, &tmp, 10);
        }

        create_job(mem_siz, prior, cmds, cmd_cnt);
        s = "{\"code\": 200, \"msg\": \"Submit job successfully\"}";
        send(fd, s, strlen(s), 0);
        break;
    }
    case '1': // job result
    {
        struct sys_info_t *sys_ptr = (struct sys_info_t *)(get_sys_mem().st);
        struct job_struct_list *ptr = sys_ptr->job_res.head;
        if (sys_ptr->job_res.size == 0)
        {
            char *s = "[]";
            send(fd, s, strlen(s), 0);
            break;
        }
        char job_info[256];
        buf[0] = '[';
        buf[1] = '\0';
        while (ptr != NULL)
        {
            sprintf(job_info, "{\"id\": %d, \"siz\": %ld, \"prior\": %d, \"status\": %d, \"msg\": \"%s\"},",
                    ptr->val->id, ptr->val->siz, ptr->val->prior, ptr->val->status, ptr->val->msg);
            strcat(buf, job_info);
            ptr = ptr->nex;
        }
        size_t len = strlen(buf);
        buf[len - 1] = ']';
        send(fd, buf, len, 0);
        break;
    }
    case '2': // user memory
    {
        struct mem_list *ptr = get_mem_list();
        char mem_info[256];
        buf[0] = '[';
        buf[1] = '\0';
        while (ptr != NULL)
        {
            sprintf(mem_info, "{\"pid\": %d, \"st\": \"%p\", \"ed\": \"%p\"},", ptr->pid, ptr->mem.st, ptr->mem.ed);
            strcat(buf, mem_info);
            ptr = ptr->nex;
        }
        size_t len = strlen(buf);
        buf[len - 1] = ']';
        send(fd, buf, len, 0);
        break;
    }
    case '3': // system memory
    {
        struct mem_list *ptr = get_sys_mem_list();
        char mem_info[256];
        buf[0] = '[';
        buf[1] = '\0';
        while (ptr != NULL)
        {
            sprintf(mem_info, "{\"pid\": %d, \"st\": \"%p\", \"ed\": \"%p\"},", ptr->pid, ptr->mem.st, ptr->mem.ed);
            strcat(buf, mem_info);
            ptr = ptr->nex;
        }
        size_t len = strlen(buf);
        buf[len - 1] = ']';
        send(fd, buf, len, 0);
        break;
    }
    case '4': // suspend process
    {
        char *s = buf + 1, *tmp;
        int cpu_id = strtol(s, &tmp, 10);
        int status = suspend_process(cpu_id);
        if (status == -1)
            s = "{\"code\": 500, \"msg\": \"Invalid CPU id\"}";
        else if (status == -2)
            s = "{\"code\": 500, \"msg\": \"No running process on this CPU now\"}";
        else
            s = "{\"code\": 200, \"msg\": \"Suspend process successfully\"}";
        send(fd, s, strlen(s), 0);
        break;
    }
    case '5': // unsuspend process
    {
        char *s = buf + 1, *tmp;
        pid_t pid = strtol(s, &tmp, 10);
        int status = unsuspend_process(pid);
        if (status == -1)
            s = "{\"code\": 500, \"msg\": \"Invalid process or it is not suspended\"}";
        else
            s = "{\"code:\": 200, \"msg\": \"Unsuspend process successfully\"}";
        send(fd, s, strlen(s), 0);
        break;
    }
    case '6': // show suspended process
    {
        struct sys_info_t *sys_ptr = (struct sys_info_t *)(get_sys_mem().st);
        struct task_struct_list *ptr = sys_ptr->suspend_queue.head;
        if (sys_ptr->suspend_queue.size == 0)
        {
            char *s = "[]";
            send(fd, s, strlen(s), 0);
            break;
        }
        buf[0] = '[';
        buf[1] = '\0';
        char task_info[512];
        while (ptr != NULL)
        {
            sprintf(task_info, "{\"pid\": %d, \"status\": %ld, \"st\": \"%p\", \"ed\": \"%p\", \"cp\": %d, \"prior\": %d, \"processor\": %d, \"job\": %d},",
                    ptr->val->pid, ptr->val->state, ptr->val->addr_limit.st, ptr->val->addr_limit.ed, ptr->val->mm.cp,
                    ptr->val->prio, ptr->val->processor, ptr->val->job->id);
            strcat(buf, task_info);
            ptr = ptr->nex;
        }
        size_t len = strlen(buf);
        buf[len - 1] = ']';
        send(fd, buf, len, 0);
        break;
    }
    case '7': // show priority queue
    {
        char *s = buf + 1, *tmp;
        int cpu_id = strtol(s, &tmp, 10);
        struct sys_info_t *sys_ptr = (struct sys_info_t *)(get_sys_mem().st);
        if (cpu_id >= sys_ptr->cpu_cnt)
        {
            s = "{}";
            send(fd, s, strlen(s), 0);
            break;
        }
        struct task_struct_head *list = sys_ptr->processors[cpu_id].rq;
        size_t len;
        buf[0] = '{';
        buf[1] = '\0';
        char task_info[512];
        for (int i = MAX_PRIOR; i >= 0; i--)
        {
            if (list[i].size == 0)
                continue;
            sprintf(task_info, "\"%d: \"[", i);
            strcat(buf, task_info);
            struct task_struct_list *ptr = list[i].head;
            while (ptr != NULL)
            {
                sprintf(task_info, "{\"pid\": %d, \"status\": %ld, \"st\": \"%p\", \"ed\": \"%p\", \"cp\": %d, \"prior\": %d, \"processor\": %d, \"job\": %d},",
                        ptr->val->pid, ptr->val->state, ptr->val->addr_limit.st, ptr->val->addr_limit.ed, ptr->val->mm.cp,
                        ptr->val->prio, ptr->val->processor, ptr->val->job->id);
                strcat(buf, task_info);
                ptr = ptr->nex;
            }
            len = strlen(buf);
            buf[len - 1] = ']';
            buf[len] = ',';
        }
        len = strlen(buf);
        if (len == 1)
        {
            s = "{}";
            send(fd, s, strlen(s), 0);
            break;
        }
        buf[len - 1] = '}';
        send(fd, buf, len, 0);
        break;
    }
    case '8': // show io queue
    {
        char *s = buf + 1, *tmp;
        int cpu_id = strtol(s, &tmp, 10);
        struct sys_info_t *sys_ptr = (struct sys_info_t *)(get_sys_mem().st);
        if (cpu_id >= sys_ptr->cpu_cnt || sys_ptr->processors[cpu_id].iorq.size == 0)
        {
            s = "[]";
            send(fd, s, strlen(s), 0);
            break;
        }
        struct task_struct_list *ptr = sys_ptr->processors[cpu_id].iorq.head;
        buf[0] = '[';
        buf[1] = '\0';
        char task_info[512];
        while (ptr != NULL)
        {
            sprintf(task_info, "{\"pid\": %d, \"status\": %ld, \"st\": \"%p\", \"ed\": \"%p\", \"cp\": %d, \"prior\": %d, \"processor\": %d, \"job\": %d},",
                    ptr->val->pid, ptr->val->state, ptr->val->addr_limit.st, ptr->val->addr_limit.ed, ptr->val->mm.cp,
                    ptr->val->prio, ptr->val->processor, ptr->val->job->id);
            strcat(buf, task_info);
            ptr = ptr->nex;
        }
        size_t len = strlen(buf);
        buf[len - 1] = ']';
        send(fd, buf, len, 0);
        break;
    }
    case '9': // show pool queue
    {
        struct sys_info_t *sys_ptr = (struct sys_info_t *)(get_sys_mem().st);
        struct job_struct_list *ptr = sys_ptr->pool_queue.head;
        if (sys_ptr->pool_queue.size == 0)
        {
            char *s = "[]";
            send(fd, s, strlen(s), 0);
            break;
        }
        char job_info[256];
        buf[0] = '[';
        buf[1] = '\0';
        while (ptr != NULL)
        {
            sprintf(job_info, "{\"id\": %d, \"siz\": %ld, \"prior\": %d, \"status\": %d, \"msg\": \"%s\"},",
                    ptr->val->id, ptr->val->siz, ptr->val->prior, ptr->val->status, ptr->val->msg);
            strcat(buf, job_info);
            ptr = ptr->nex;
        }
        size_t len = strlen(buf);
        buf[len - 1] = ']';
        send(fd, buf, len, 0);
        break;
    }
    case 'a': // show processor info
    {
        char *s = buf + 1, *tmp;
        int cpu_id = strtol(s, &tmp, 10);
        struct sys_info_t *sys_ptr = (struct sys_info_t *)(get_sys_mem().st);
        if (cpu_id >= sys_ptr->cpu_cnt)
        {
            s = "{\"code\": 500, \"msg\": \"Invalid CPU id\"}";
            send(fd, s, strlen(s), 0);
            break;
        }
        struct processor *ptr = &(sys_ptr->processors[cpu_id]);
        buf[0] = '\0';
        sprintf(buf, "{\"code\": 200, \"id\": %d, \"task_cnt\": %d, \"cur\": \"%p\"}", ptr->cores_id, ptr->tasks_cnt, ptr->cur);
        send(fd, buf, strlen(buf), 0);
        break;
    }
    case 'b': // show system info
    {
        struct sys_info_t *sys_ptr = (struct sys_info_t *)(get_sys_mem().st);
        buf[0] = '\0';
        sprintf(buf, "{\"cpu_cnt\": %d, \"pid_cnt\": %d, \"proc_cnt\": %d, \"run_proc_cnt\": %d, \"job_cnt\": %d}",
                sys_ptr->cpu_cnt, sys_ptr->pid_cnt, sys_ptr->proc_cnt, sys_ptr->run_proc_cnt, sys_ptr->job_cnt);
        send(fd, buf, strlen(buf), 0);
        break;
    }
    default:
    {
        char *s = "{\"code\": 500, \"msg\": \"Invalid command\"}";
        send(fd, s, strlen(s), 0);
    }
    }
ed:
    close(fd);
}

int init_interface()
{
    struct sys_info_t *sys_ptr = (struct sys_info_t *)(get_sys_mem()).st;
    boss_fd = socket(AF_INET, SOCK_STREAM, 0);
    if (boss_fd == -1)
        return -1;
    struct sockaddr_in server_addr;
    server_addr.sin_family = AF_INET;
    server_addr.sin_addr.s_addr = htonl(INADDR_ANY);
    server_addr.sin_port = htons(PORT);
    int opt = 1;
    if (setsockopt(boss_fd, SOL_SOCKET, SO_REUSEADDR, (const void *)&opt, sizeof(opt)) < 0)
        return -1;
    if (bind(boss_fd, (struct sockaddr *)&server_addr, sizeof(server_addr)) == -1)
        return -1;
    if (listen(boss_fd, 100) == -1)
        return -1;
    while (1)
    {
        int fd = accept(boss_fd, (struct sockaddr *)NULL, NULL);
        if (fd == -1)
            return -1;
        pthread_t pid;
        if (pthread_create(&pid, NULL, (void *)interface_work, (void *)(&fd)) != 0)
            return -1;
        pthread_detach(pid);
    }
    return 0;
}

int create_job(size_t mem_size, int prior, struct cmd_struct *cmd_seq, int cmd_cnt)
{
    struct sys_info_t *sys_ptr = (struct sys_info_t *)(get_sys_mem().st);

    struct job_struct *job = (struct job_struct *)malloc(sizeof(struct job_struct));
    job->id = ++(sys_ptr->job_cnt);
    job->cmd_cnt = cmd_cnt;
    job->siz = mem_size;
    job->prior = prior;
    job->cmd = (struct cmd_struct *)malloc(cmd_cnt * sizeof(struct cmd_struct));
    job->status = JOB_READY;
    job->msg = "Job is ready to run";
    memcpy(job->cmd, cmd_seq, cmd_cnt * sizeof(struct cmd_struct));

    struct job_struct_list *job_list = (struct job_struct_list *)_malloc(0, sizeof(struct job_struct_list));
    job_list->nex = NULL;
    job_list->val = job;
    struct job_struct_head *list = &(sys_ptr->pool_queue);
    if (list->size == 0)
        list->head = list->tail = job_list;
    else
        list->tail->nex = job_list, list->tail = job_list;
    ++(list->size);

    // try to run the job immediately
    // if ready queue is empty, this call can make CPU work
    do_long_schedule(JOB_READY);

    return 0;
}

int create_process(struct job_struct *job)
{
    struct sys_info_t *sys_ptr = (struct sys_info_t *)(get_sys_mem().st);
    if ((job->cmd_cnt * sizeof(struct cmd_struct)) > job->siz)
    {
        job->status = JOB_FAILED;
        job->msg = "Command size is less than the memory size";
        push_job_res(job);
        return -1;
    }
    int selected = 0;
    for (int i = 1; i < sys_ptr->cpu_cnt; i++)
        if (sys_ptr->processors[i].tasks_cnt < sys_ptr->processors[selected].tasks_cnt)
            selected = i;
    struct task_struct *task = (struct task_struct *)_malloc(0, sizeof(struct task_struct));
    if (task == NULL)
    {
        job->status = JOB_FAILED;
        job->msg = "Out of memory";
        push_job_res(job);
        return -2;
    }
    task->pid = ++(sys_ptr->pid_cnt);
    task->prio = job->prior;
    task->state = TASK_READY;
    char *ptr = (char *)_malloc(task->pid, job->siz);
    if (ptr == NULL)
    {
        _sys_free(task);
        job->status = JOB_FAILED;
        job->msg = "Out of memory";
        push_job_res(job);
        return -2;
    }
    task->addr_limit.st = ptr;
    task->addr_limit.ed = ptr + job->siz;
    memcpy(task->addr_limit.st, (char *)job->cmd, job->cmd_cnt * sizeof(struct cmd_struct));
    task->job = job;
    task->mm.start_code = 0;
    task->mm.end_code = task->mm.start_code + job->cmd_cnt * sizeof(struct cmd_struct);
    task->mm.start_data = task->mm.end_code;
    task->mm.end_data = task->addr_limit.ed - task->addr_limit.st;
    task->mm.cp = 0;
    task->processor = selected;

    printf("[Shell] Process %d has been created on CPU%d (space: %p - %p)\n", task->pid, selected, task->addr_limit.st, task->addr_limit.ed);

    ++(sys_ptr->processors[selected].tasks_cnt);
    ++(sys_ptr->run_proc_cnt);

    if (pthread_mutex_lock(&sys_ptr->processors[selected].lock) != 0)
        return -3;
    push_back_task(&sys_ptr->processors[selected].rq[job->prior], task);
    if (pthread_mutex_unlock(&sys_ptr->processors[selected].lock) != 0)
        return -3;

    job->status = JOB_RUNNING;
    job->msg = "Job is running";

    return 0;
}

int suspend_process(int core_id)
{
    struct sys_info_t *sys_ptr = (struct sys_info_t *)(get_sys_mem().st);
    if (core_id >= sys_ptr->cpu_cnt)
        return -1;
    struct processor *pro = &(sys_ptr->processors[core_id]);
    struct task_struct *task = pro->cur;
    if (task == NULL)
        return -2;
    task->state = TASK_SUSPEND;
    pro->mid_schedule_status = 1;
    pthread_kill(pro->tid, SIGALRM); // suspend current process immediately
    return 0;
}

int unsuspend_process(pid_t pid)
{
    struct sys_info_t *sys_ptr = (struct sys_info_t *)(get_sys_mem().st);
    struct task_struct_list *ptr = sys_ptr->suspend_queue.head;
    while (ptr != NULL)
    {
        if (ptr->val->pid == pid)
        {
            if (ptr->val->state == TASK_SUSPEND)
                ptr->val->state = TASK_READY;
            else
                ptr->val->state = TASK_INTERRUPTIBLE;
            (&(sys_ptr->processors[ptr->val->processor]))->mid_schedule_status = 1;
            // no need to run middle schedule immediately
            return 0;
        }
        ptr = ptr->nex;
    }
    return -1;
}