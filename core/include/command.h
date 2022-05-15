#ifndef _COMMAND_H_
#define _COMMAND_H_
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

#endif