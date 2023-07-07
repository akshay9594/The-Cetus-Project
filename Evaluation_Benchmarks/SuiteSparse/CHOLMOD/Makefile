#-------------------------------------------------------------------------------
# CHOLMOD Makefile
#-------------------------------------------------------------------------------

SUITESPARSE ?= $(realpath $(CURDIR)/..)
export SUITESPARSE

default: all

include ../SuiteSparse_config/SuiteSparse_config.mk

# Compile the C-callable libraries and the Demo programs.
all:
	( cd Lib ; $(MAKE) --jobs=$(JOBS) )
	( cd Demo ; $(MAKE) )

# Compile the C-callable libraries only.
library:
	( cd Lib ; $(MAKE) --jobs=$(JOBS) )

# compile the static libraries only
static:
	( cd Lib    ; $(MAKE) static --jobs=$(JOBS) )

# Remove all files not in the original distribution
purge:
	( cd Tcov ; $(MAKE) purge )
	( cd Lib ; $(MAKE) purge )
	( cd Valgrind ; $(MAKE) dopurge )
	( cd Demo ; $(MAKE) purge )
	( cd Doc ; $(MAKE) purge )
	( cd MATLAB ; $(RM) $(CLEAN) rename.h *.mex* GKlib_util.c GKlib_graph.c )

# Remove all files not in the original distribution, except keep the 
# compiled libraries.
clean:
	( cd Lib ; $(MAKE) clean )
	( cd Demo ; $(MAKE) clean )

distclean: purge

ccode: all

# Compile the C-callable libraries and the Demo programs.
demos:
	( cd Demo ; $(MAKE) )

# create PDF documents for the original distribution
docs:
	( cd Doc    ; $(MAKE) )

# install CHOLMOD
install:
	( cd Lib ; $(MAKE) install )

# uninstall CHOLMOD
uninstall:
	( cd Lib ; $(MAKE) uninstall )
