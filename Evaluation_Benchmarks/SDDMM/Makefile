#BHEADER****************************************************************
# (c) 2007   The Regents of the University of California               *
#                                                                      *
# See the file COPYRIGHT_and_DISCLAIMER for a complete copyright       *
# notice and disclaimer.                                               *
#                                                                      *
#EHEADER****************************************************************
CC       = gcc -fopenmp

CFLAGS   = -O3 -g

LDFLAGS  = -lm

PROG     = sddmm

OBJS     = main.o

all : $(PROG)

$(PROG) : $(OBJS)
	${CC} $(CFLAGS) -o $(PROG) $(OBJS) $(LDFLAGS)


clean :
	rm -f *.o $(PROG) core job.out *~ 


.SUFFIXES : .o  .c

#*****************************************************************
#* Rules for C object file dependencies
#*****************************************************************
.c.o :
	$(CC) -o $@ -c $(CFLAGS) $*.c 






