/* halt.c
 *	Simple program to test whether running a user program works.
 *	
 *	Just do a "syscall" that shuts down the OS.
 *
 * 	NOTE: for some reason, user programs with global data structures 
 *	sometimes haven't worked in the Nachos environment.  So be careful
 *	out there!  One option is to allocate data structures as 
 * 	automatics within a procedure, but if you do this, you have to
 *	be careful to allocate a big enough stack to hold the automatics!
 */

#include "syscall.h"

int main()
{
    char * s = "abc.txt";
//    int fd = creat(s);
    int fd = open(s);
    unlink(s);
    char b[6];
    int l = read(fd, b, 6);
//    close(fd);
//    fd = creat(s);
    char c[] = "fedcba";   	
    write(fd, c, l);
    halt();
    /* not reached */
}
