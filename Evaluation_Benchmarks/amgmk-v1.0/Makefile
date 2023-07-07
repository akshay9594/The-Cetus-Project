#BHEADER****************************************************************
# (c) 2007   The Regents of the University of California               *
#                                                                      *
# See the file COPYRIGHT_and_DISCLAIMER for a complete copyright       *
# notice and disclaimer.                                               *
#                                                                      *
#EHEADER****************************************************************
CC       = gcc -fopenmp

CFLAGS   = -O3 

LDFLAGS  = -lm

PROG     = AMGMk

OBJS     = main.o \
           csr_matrix.o csr_matvec.o  \
           laplace.o relax.o \
           hypre_error.o hypre_memory.o \
           vector.o

all : $(PROG)

$(PROG) : $(OBJS)
	${CC} -o $(PROG) $(OBJS) $(LDFLAGS)


clean :
	rm -f *.o $(PROG) core job.out *~ 


.SUFFIXES : .o  .c

#*****************************************************************
#* Rules for C object file dependencies
#*****************************************************************
.c.o :
	$(CC) -o $@ -c $(CFLAGS) $*.c 






