# 操作系统实验

文章已同步至[个人博客](https://magicalsheep.cn/3566838299/ "进程调度器")，可能会获得更好的阅读体验。编译及运行指南见各子文件夹内部。

- [需求分析](#需求分析)
  - [模块和功能划分](#模块和功能划分)
  - [作业与进程](#作业与进程)
  - [进程状态](#进程状态)
  - [进程运行](#进程运行)
  - [时钟中断](#时钟中断)
- [总体设计](#总体设计)
  - [内存管理](#内存管理)
  - [I/O设备](#io设备)
  - [调度过程](#调度过程)
- [详细设计](#详细设计)
  - [内存分配与回收函数](#内存分配与回收函数)
  - [进程执行函数](#进程执行函数)
  - [短程调度函数](#短程调度函数)
  - [中程调度函数](#中程调度函数)
  - [长程调度函数](#长程调度函数)
  - [前后端通信接口](#前后端通信接口)
- [实验运行结果](#实验运行结果)

# 需求分析

## 模块和功能划分

由于存在图形用户界面需求，首先可将整体程序划分为两个部分：作为核心程序的后端和作为图形用户界面的前端，并使用Socket通信方式连接前后端程序。前端的编写并非该实验的重点，因此，接下来我们将只讨论关于核心的设计与编写问题。

核心从功能上可被分为两个模块，处理机调度模块及内存管理模块。处理机调度模块由三个部分组成：长程调度（高级调度、作业调度）、中程调度、短程调度（进程调度）。我们将看到，在编写处理机调度模块时，区分这三类调度是相当重要的，这将避免陷入编写混乱。以下几个问题则是区分这三者的关键：

- 调度对象是谁？
- 调度发生时机在何时？
- 调度完成了什么工作？

相比于处理机调度模块，内存模块在实现上则要简单许多，但仍有一些重要的概念需要理清。从需求上看，当进程被创建时，将在主存上分配进程地址空间，并且这一块地址空间需由我们手动进行维护。因此，程序需要从操作系统中预申请一块大的内存空间，并在该内存空间中划分出一整块作为用户空间内存，建立内核数据结构维护内存块分配信息。

另一方面，内核数据结构（如PCB链表等）本身也需要占据内存，但它们并不占用用户空间内存，而是存放于系统空间之中，从主存中划分一块固定大小的区域作为系统空间地址也是必要的。

出于简化实现的目的，我们将认为预先申请的一整块内存空间为主存，而其他后续申请的内存空间为外存。

## 作业与进程

由于高级调度的存在，该系统事实上存在一个批处理系统，该系统将采用一种作业调度算法来将作业实例化为运行进程，供进程调度器支配，而完成这一作业调度过程的即为长程调度器。

由于受到现代实时操作系统的影响——它们一般并不存在高级调度过程，用户提交的程序将直接成为一个新的进程进入内存当中，我们往往会忽略长程调度器在该实验中的存在，进一步错误地认为其工作所依赖的后备队列存在于内存当中。这样，在设计实现短程调度器的过程中便会产生混乱。

事实上，程序被提交后将作为一个作业进入长程调度器的后备队列当中，并存放于外存。当长程调度器按照其作业调度算法认为该程序该被执行时，才从外存中读入作业信息，并填写PCB生成进程实例，加入短程调度器的就绪队列。进程数据将存放于内存。

实验指导书没有要求高级调度中所采用的作业调度算法，为了简单起见，我们将采用先来先服务算法实现。

## 进程状态

初始和终止状态并不会影响我们的实现，在接下来的讨论中我们先将它们排除。则经典的三个进程状态为就绪态、运行态、阻塞态，它们之间的状态转换关系可用下图表示：

![](https://magical-sheep.oss-cn-hangzhou.aliyuncs.com/2022-5-19/107.png "三状态转换图")

从需求中可知，该系统需支持挂起与解挂功能，这就意味着我们还需要引入就绪挂起态和阻塞挂起态，此时，五个状态之间的转换关系如下图所示：

![](https://magical-sheep.oss-cn-hangzhou.aliyuncs.com/2022-5-19/108.png "五状态转换图")

## 进程运行

实验指导书给出了PCB的部分内容，其中含有要求运行时间一项，进程运行的过程即是递减该值的过程。但这其实隐含了一个假设：整个程序不存在任何陷入外部的阻塞I/O指令，其运行时间中的每一刻都需要占用处理机时间。这种假设可以被视为一种简化，即忽略考虑阻塞态及阻塞挂起态。

在本程序中，出于完整实现进程状态转换图的目的，我们选择考虑因阻塞I/O操作而导致的让出处理机时间问题。因此，隐含的假设不能成立，进程的运行将由一系列的普通指令操作或I/O指令操作组成，其中I/O指令操作将陷入外部I/O设备阻塞，运行不占用处理机时间。

进一步地，既然我们已为所产生的进程手动维护一块进程空间内存，那么类似于真实进程中存在固定代码段内存区域，我们可以在维护的进程空间内存中存放程序运行所需要的一系列指令。由于不存在内存和寄存器信息，在进程保存与恢复现场的操作中，唯一需要的信息只有程序段执行位置的偏移量，因此，除了代码段相对于进程空间起始地址的偏移量外，PCB还需记录程序段执行位置的偏移量。

这里还需要提及实验指导书中所提及的另一个问题，即进程属性中的同步进程。在我看来，进程间协作的同步应当由应用开发者采用各种进程间通信策略来实现，或共享内存，或管道通信，在通信基础上实现信号量机制来确保同步，而并非应在操作系统调度层面上确保进程同步。

如果考虑死锁问题，那么操作系统是否需要确保进程之间不会发生死锁也并不是一个具有标准答案的问题，采用鸵鸟算法也是进程间死锁问题的一种解决方式。如果尝试在操作系统层面检测和移除进程间发生的死锁，则需要对进程关系建图，这会增加实验的复杂度。因此，该程序中的所有进程均为独立进程，不对同步进程做任何处理。

## 时钟中断

我们知道，对于实际操作系统而言，存在来自CPU的硬件时钟中断支持，短程调度器在处理时钟中断的函数中被调用。没有硬件时钟中断支持的平台上无法实现抢占式调度算法，因为操作系统无法打断正在运行的程序来获得控制权。

考虑到Linux下可通过设置timer来获得时钟软中断信号`SIGALRM`，我们可以尝试利用该软中断信号来作为硬件时钟中断的模拟，短程调度函数将在该软中断信号处理函数中被调用。由于Linux下的`sleep`函数通过设置定时器后挂起而实现，它同样会受到`SIGALRM`信号的中断，并返回剩余秒数，因此利用该信号实现时钟中断，从而打断正在运行的程序是可行的。

# 总体设计

## 内存管理

由前文所述分析可知，程序在真实操作系统中所占用的内存应当作如下划分。

![](https://magical-sheep.oss-cn-hangzhou.aliyuncs.com/2022-5-19/101.png "内存分布")

首先考虑系统空间的内存分布情况。系统空间由静态数据与动态数据组成，静态数据包括系统本身的配置属性，如处理机个数、允许在内存中运行的程序道数、当前作业数等，以及部分存放于内存或外存的内核数据结构的地址，如后备队列地址、各处理机信息地址等。由此可以构造出系统固定内存的结构体如下。

```C
struct sys_info_t
{
    int cpu_cnt, pid_cnt, proc_cnt, run_proc_cnt, job_cnt;
    struct job_struct_head pool_queue, job_res;
    struct task_struct_head suspend_queue;
    struct processor *processors;
};
```

系统固定内存之外的部分则用于动态管理内核数据结构，如链表节点等。综上可得系统空间的内存分布如下图所示。

![](https://magical-sheep.oss-cn-hangzhou.aliyuncs.com/2022-5-19/102.png "系统内存空间分布")

接下来考虑用户空间的内存分布情况。对于本实验而言，用户空间仅用于存放进程运行数据，动态管理其申请与回收，因此，讨论用户空间的内存分布即是讨论进程空间的组成。对于一个运行中的进程而言，我们可大致地将其分为代码段与数据段两部分（尽管在该实验中数据段可有可无），存放于系统空间中的PCB信息将确定代码段执行位置的偏移量，代码段中的指令可以访问数据段中所存储的信息。

由此我们可以得到PCB中对进程空间的描述结构如下。

```C
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
```

由于使用交换技术，进程在换入换出内存的过程中将会改变其所拥有的内存块起止地址，因此对于单个进程空间中的内存分布情况而言需使用偏移量存储（即变址），当内存块起始地址（即基址）确定后，进程空间内各部分的实际地址也就确定了下来。

加上进程调度相关的其他进程属性，我们可以得到完整的PCB结构体如下。

```C
#define TASK_RUNNING 0
#define TASK_READY 1
#define TASK_INTERRUPTIBLE 2
#define TASK_STOPPED 3
#define TASK_SUSPEND 4
#define TASK_IO_SUSPEND 5

struct task_struct
{
    volatile long state;
    pid_t pid;
    struct mm_segment_t addr_limit;
    struct mm_struct mm;
    int prio, processor;
    struct job_struct *job;
};
```

用户空间内存可视化如下图所示。

![](https://magical-sheep.oss-cn-hangzhou.aliyuncs.com/2022-5-19/103.png "用户内存空间分布")

至此，各部分的内存分布大体上已有了比较清晰的结构。为了能够更好地描述后续的调度流程，接下来我们将对其中一些组成部分的内存结构作出阐述，它们分别是处理机和作业。

处理机拥有一些基本的属性信息，如核心号、正在处理的任务数、正在运行的进程指针等，这些属性将在后续实现调度算法的过程中被应用上。处理机最为关键的属性为就绪队列和阻塞队列（如果实现了阻塞态），由于实验要求使用抢占式优先权调度算法进行进程调度，因此可以按优先权为键划分桶，每个桶中存放该优先级的就绪进程所组成的链表。我们可以得到如下处理机结构描述。

```C
#define MAX_PRIOR 60

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
```

这些信息存放于系统空间内存当中，在程序启动时被初始化空间，每一个处理机都有一份这样的内存结构，可视化后如下图所示。

![](https://magical-sheep.oss-cn-hangzhou.aliyuncs.com/2022-5-19/104.png "处理机内存分布")

作业结构要相对简单一些，一个作业由其所需的资源描述和程序正文组成，程序正文又由一系列指令组成，不难得到其结构体描述如下。

```C
#define JOB_RUNNING 0
#define JOB_READY 1
#define JOB_COMPLETED 2
#define JOB_FAILED 3

#define COMMON_CMD 0
#define IO_CMD 1

/**
 * Description of a command
 *
 * The program code consists of a series of commands
 */
struct cmd_struct
{
    unsigned int type;
    unsigned long times;
};

struct job_struct
{
    int id;
    size_t siz;
    int prior, cmd_cnt, status;
    struct cmd_struct *cmd;
    char *msg;
};
```

## I/O设备

当进程执行到I/O指令时将进入处理机的阻塞队列，并释放处理机占用时间，此时该指令将陷入外部的I/O设备进行执行。为此，我们需要抽象一个简单的I/O处理设备，它将拥有一个自己的处理队列，每次从处理队列中取出I/O任务，执行，完成后通过中断通知处理机将相应进程移出阻塞队列，加入就绪队列接受调度，自身则继续处理下一个I/O任务。为了简单起见，我们将为全局设立一个固定的I/O设备，所有进程的I/O指令将通过该设备进行执行。

![](https://magical-sheep.oss-cn-hangzhou.aliyuncs.com/2022-5-19/105.png "I/O处理过程")

由于该设备从概念上属于外部设备，其内存由其自身管理，因此在本程序中其所有的内存占用都存放于外存当中。

## 调度过程

作业经用户输入后，将进入批处理系统的后备队列，同时立即启动长程调度器，以避免处理机空转。但长程调度器并不保证提交的作业将立即被实例化为进程加入就绪队列，而是根据当前内存中正在运行的进程数与最大道数之间的关系，按照先来先服务算法处理后备队列。一旦任一处理机中有进程结束，长程调度器将再次启动。

需要注意的是，当进程被用户手动挂起，从内存中清除后，并没有立即调用长程调度，此处属于我的设计失误。这种考虑的出发点是基于本程序所有将未完成进程换出内存的操作都仅由用户主动发起（挂起），系统本身不会因为内存不足而主动将不活跃进程的内存换出（实验没有具体要求该功能，实现它会加大总体复杂度），因此，如果挂起进程后调用了长程调度，长程调度在内存中创建了新的进程，那么当用户解挂先前进程时将可能会面临内存不足以及超过内存所允许的最大程序道数问题。然而事实上即使采用了挂起后不进行长程调度的策略，也依然无法避免该问题——长程调度还可能因为加入新的作业/其他处理机结束了进程而被调用。

最为核心和执行最为频繁的是短程调度器，也即进程调度器。处理机将在每个时钟中断到达的时候中断正在执行的进程，保存该进程的现场，将优先级递减后重新加入就绪队列，然后调用短程调度函数。短程调度将首先检查处理机阻塞队列中的进程状况（事实上这应当属于中断处理函数的内容），将已完成I/O的进程加入就绪队列。然后短程调度器将从优先级最高的就绪队列链表中取出头元素，并交由处理机进行执行。

处理机的执行过程是比较简单的，它根据PCB中所存储的进程空间信息和代码段执行位置的偏移量确定下一条运行的指令。如果请求的指令为I/O操作，则会立即保存现场并返回处理机，由处理机将其加入阻塞队列并将I/O指令交由外部I/O设备执行。

中程调度与进程的挂起和解挂功能相关，实现中程调度最关键的问题为“何时进行中程调度”。我们知道，挂起是一种主动换出内存的操作，由于用户的不确定性，这一操作将可能从任何时刻介入，如果简单的采用多线程来模拟实现，将引起大量的同步问题。在现实中，单一处理机在同一时刻只能执行长中短程调度三者中的其中一种，而短程调度会在每一个时钟周期中被调用。这意味着，当某种条件满足之后，时钟周期内执行的短程调度会进而调用中长程调度函数。上文我们已经看见，当新作业加入、进程结束两种事件发生后，长程调度将被调用。类似的，我们为每一个处理机设立一个中程调度的标志位，当需要进行中程调度时，将标志位置1，并向处理机发送时钟中断信号，引起短程调度的立即执行，那么中程调度也就同时被调用了。

发生在中程调度中的另一个问题是进程状态的转换。挂起态存在两种类别：就绪挂起和阻塞挂起，从前文所附状态转换图来看，处于阻塞态（此时指令已交由外部I/O设备执行）的运行进程被挂起后将进入阻塞挂起态，并在阻塞事件结束后自动转换为就绪挂起态。解挂就绪挂起态将使进程进入就绪队列，而解挂阻塞挂起态将使进程回到处理机的阻塞队列当中。这意味着，当一个挂起进程的主动操作被发起后，正确地为它赋予状态值是很重要的。然而，在代码实践中，正确地区分就绪挂起和阻塞挂起状态并在中程调度时赋值并非那么容易。

程序整体的三级调度过程可用下图表示。

![](https://magical-sheep.oss-cn-hangzhou.aliyuncs.com/2022-5-19/106.png "三级调度图")

# 详细设计

## 内存分配与回收函数

将内存块组织成为链表，按照首次适应算法对其进行分配，即顺序扫描链表直到第一个大于等于所需内存的空闲块。回收时顺序扫描链表直到目标地址块，然后检查前后两个相邻节点是否空闲，若空闲则将其合并。

```C
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
```

## 进程执行函数

按照恢复现场、执行指令、保存现场三个步骤进行处理。

```C
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
```

## 短程调度函数

先检查中程调度标志位，若被设置则先执行中程调度，否则检查阻塞队列后选择优先级最高的进程开始执行。

```C
/**
 * Short scheduler function entry.
 *
 * Check whether there is an I/O process ready
 * and then switch a process from the priority queue to exec.
 */
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
```

## 中程调度函数

中程调度函数的内容较多，首先检查是否需要从挂起队列中移除进程，其次分别检查就绪队列与阻塞队列中是否有需要移入挂起队列的进程，最后重置标志位。另外，由于中程调度需要执行内存的换入换出操作，在将进程从挂起队列移出时，需要为其重新分配新的进程空间地址，同时从外存中复制PCB信息到内存当中；将进程加入到挂起队列中同理。由于代码量较多，故不在此处附上。

注意，该函数为单例，需使用互斥锁保护。

## 长程调度函数

长程调度函数较为简单，只需判断内存中程序运行的道数，从而决定是否从后备队列中取出作业、选择合适的处理机，创建进程并加入就绪队列待调度。该函数同样需要使用互斥锁保护。

```C
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
```

## 前后端通信接口

后端的运行核心是使用GNU C编写并运行于Linux系统之上的，而面向用户的图形界面程序则使用Java编写且运行于Windows之上，从通信角度来看属于主机间通信（如果将核心运行于WSL中，或许可以考虑使用Windows 10已支持的Unix套接字进行进程间通信，但表现似乎并不太好，也不够通用），因此采用Socket网络通信的方式，以服务器-客户机的方式进行沟通。

最初该部分代码使用裸的Socket套接字进行编写，完全的BIO模式与复制读写，在前端极其频繁的轮询之下，通信几乎无法正常工作。考虑到通信部分并非该实验的核心要求，于是引入ZeroMQ作为通信层的基础设施，从而得以改善该问题。然而，前端仍然存在着因高频多线程并发轮询而导致的CPU占用过高的问题，就当是特性吧。

该部分代码位于核心的`src/interface.c`文件当中。

# 实验运行结果

![](https://magical-sheep.oss-cn-hangzhou.aliyuncs.com/2022-5-19/54.png "运行结果1")

![](https://magical-sheep.oss-cn-hangzhou.aliyuncs.com/2022-5-19/52.png "运行结果2")

![](https://magical-sheep.oss-cn-hangzhou.aliyuncs.com/2022-5-19/51.png "运行结果3")

![](https://magical-sheep.oss-cn-hangzhou.aliyuncs.com/2022-5-19/53.png "运行结果4")