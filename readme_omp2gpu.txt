RELEASE
-------
Omp2gpu 0.31 (July. 8, 2011)

Omp2gpu is a OpenMP-to-CUDA translator built on top of Cetus compiler
infrastructure. The translation framework 1) supports fully automatic
OpenMP-to-CUDA translation with various optimizations
and 2) includes tuning capabilities for generating, pruning, and 
navigating the search space of compilation variants.


FEATURES/UPDATES
----------------
******************************************************
* Compiler flags used for OpenMP-to-CUDA translation *
******************************************************
###################################
# Safe, always-beneficial Options #
###################################
  -cudaMemTrOptLevel=N  where N = 1, 2, or 3
    CUDA CPU-GPU memory transfer optimization level (0-4) (default is 2)
    CF1: If N > 3, aggressive, may-unsafe optimizations are applied.
    CF2: If N >= 3, "cudaMallocOptLevel" flag is automatically set to 1.
    CF3: Optimization level of "globalGMallocOpt" flag is also controlled
         by this flag.
#############################################################################
# Safe, always-beneficial Options, but resources may limit its application. #
#############################################################################
  -useGlobalGMalloc
    Allocate GPU variables as global variables to reduce memory transfers
    between CPU and GPU
  -useMatrixTranspose
    Apply MatrixTranspose optimization in OMP2CUDA translation.
    This optimization is applicable when a threadprivate variable is 
      an array of 1 dimension.
  -useMallocPitch
    Use cudaMallocPitch() in OMP2CUDA translation.
    This transformation is applicable when a shared variable is 
      an array of 2 dimensions.
#############################################################
# May-beneficial Options, which interact with other options #
#############################################################
  -useLoopCollapse
    Apply LoopCollapse optimization in OMP2GPU translation.
  -useParallelLoopSwap
    Apply ParallelLoopSwap optimization in OMP2GPU translation.
  -useUnrollingOnReduction
    Apply loop unrolling optimization for in-block reduction
      in OMP2GPU translation; to apply this opt, thread block size, 
      BLOCK_SIZE = 2^m and m > 0. 
    Each kernel region can be tuned using "noreductionunroll"
      clause.
###################################################################
# Always-beneficial Options, but inaccuracy of the analysis may   #
# break correctness (aggressive, unsafe options)                  #
###################################################################
  -globalGMallocOpt
    Optimize global GPU variable allocation to reduce memory transfers;
      to use this option, "useGlobalGMalloc" flag should be on.
    CF1: If "cudaMemTrOptLevel" > 3, aggressive, may-unsafe optimizations 
      are applied.
  -cudaMemTrOptLevel=N  where N = 4
    CUDA CPU-GPU memory transfer optimization level (0-4) (default is 2)
    CF1: If N < 4, always-beneficial, safe optimizations are applied.
    CF2: If N >= 3, "cudaMallocOptLevel" flag is automatically set to 1.
  -cudaMallocOptLevel=1
    CUDA Malloc optimization level (0-1) (default is 0)
    CF1: If "cudaMemTrOptLevel" flag has value bigger than 2, 
      this flag is automatically set to 1.
####################################################################
# Safe, always-beneficial Options, but user's approval is required #
####################################################################
  -assumeNonZeroTripLoops
    Assume that all loops have non-zero iterations. If a user approves
      this, more accurate analyses can be applied.
##############################################################
# May-beneficial options, which interact with other options. #
# These options are not needed if kernel-specific options    #
# are used.                                                  #       
##############################################################
  -shrdSclrCachingOnReg
    Cache shared scalar variables onto GPU registers.
    CF1: Each kernel region can be tuned using registerRO, registerRW,
      and noregister clauses.
  -shrdArryElmtCachingOnReg
    Cache shared array elements onto GPU registers.
    CF1: Each kernel region can be tuned using registerRO, registerRW,
      and noregister clauses.
  -shrdSclrCachingOnSM
    Cache shared scalar variables onto GPU shared memory.
    CF1: Each kernel region can be tuned using sharedRO, sharedRW,
      and noshared clauses.
  -prvtArryCachingOnSM
    Cache private array variables onto GPU shared memory.
    CF1: Each kernel region can be tuned using sharedRO, sharedRW,
      and noshared clauses.
  -shrdArryCachingOnTM
    Cache 1-dimensional, R/O shared array variables onto GPU Texture memory.
    CF1: Each kernel region can be tuned using texture and notexture clauses.
  - shrdSclrCachingOnConst
    Cache R/O shared scalar variables onto GPU constant memory
  - shrdArryCachingOnConst
    Cache R/O shared array variables onto GPU constant memory
####################################################
# Non-tunable options, but user may need to apply  #
# some of these to generate correct output code.   #
####################################################
  -disableCritical2ReductionConv
    Disable Critical-to-reduction conversion pass.
  -doNotRemoveUnusedSymbols
    Do not remove unused local symbols in procedures.
  -UEPRemovalOptLevel=N,
    Optimization level (0-3) to remove upwardly exposed private (UEP)
      variables (default is 0, which does not apply this optimization).
    CF1: This optimization may be unsafe; this should be enabled only if 
      UEP problems occur, and programmer should verify the correctness 
      manually. 
  -forceSyncKernelCall  
    If enabled, cudaThreadSynchronize() call is inserted right after
      each kernel call to force explicit synchronization; useful for debugging.
##################################
# Options for CUDA configuration #
##################################
  -cudaThreadBlockSize=number
    Size of CUDA thread block (default value = 128)
  -maxNumOfCudaThreadBlocks=N 
    Maximum number of CUDA ThreadBlocks; if this option is on,
      tiling transformation code is added to fit work partition
      into the thread batching specified by this flag and 
      "cudaThreadBlockSize" flag. 
  -cudaMaxGridDimSize=size
    Maximum size of each dimension of a grid of thread blocks
    (System maximum = 65535)
  -cudaGridDimSize=size
    Size of each dimension of a grid of thread blocks, when thread
    block ID is a 2-dimensional array (max = 65535, default value = 10000).
  -cudaGlobalMemSize=size
    Size of CUDA global memory in bytes (default value = 1600000000)
    Used for debugging
  - doNotUseCUTIL
    Do not use CUTIL CUDA SDK libraries when genarating CUDA code.
	Without this option, CUDA SDK is needed to compile the output
	CUDA code.
    If this option is on, addCudaErrorCheckingCode is disabled.
######################
# Options for tuning #
######################
  -cudaConfFile=filename
    Name of the file that contains CUDA configuration parameters.
    Any valid OpenMP-to-GPU compiler flags can be put in the file;
      one flag per line, and lines starting with '#' will be ignored.
    CF1: Using this file, a user does not have to specify compiler flags 
      of interest in a command-line input.
    CF2: The file should exist in the current directory.
  -cudaUserDirectiveFile=filename
    Name of the file that contains user-directives. 
    Using this file, user can annotate each OpenMP parallel region
      without inserting user-directives directly into the input source 
      code. 
    Each line in the file has the following format:
      kernelid(id) procname(pname) [clause[[,] clause]...]
        where clause is one of the OpenMPC clauses used in "cuda gpurun"
        directives (explained in the next section)
    kernelid and procname clauses are automatically inserted by translator
      for each eligible kernel region. To find kernel IDs for kernel 
      regions in an input program, a user have to run the translator 
      at least once. (Running translator with OmpKernelSplitOnly option will 
      show kernelID information with minimal changes on the input program.)
    The file should exist in the current directory.
  -extractTuningParameters=filename
    Extract tuning parameters; output will be stored in the specified
    file. (Default is TuningOptions.txt)
    The generated file contains information on tuning parameters
    (compiler flags and user-directives) applicable to the current input program.
  -genTuningConfFiles=tuningdir
    Generate tuning configuration files and/or user-directive files.
    Each tuning configuration file (and a user-directive file) constitutes
    a point in a optimization search space, defined for the current 
    input program.
    A user can create a set of CUDA code variants, applied with different 
    optimizations, using the generated output files.
    Output files will be stored in the specified directory. 
    (Default is tuning_conf)
  -tuningLevel=N
    Set tuning level when genTuningConfFiles option is on;
    N = 1 (exhaustive search on program-level tuning options, default)
    N = 2 (exhaustive search on kernel-level tuning options)
  -defaultTuningConfFile=filename
    Name of the file that contains default CUDA tuning configurations,
      such as flags that are always applied and flags that are excluded.
    This file is used to set up an optimization search space, where
      a tuning system navigates to find the best performance.
    If the file does no exist, system-default setting will be used.
    (Default file name is cudaTuning.config) 
#########################
# Options for debugging #
#########################
  -showGResidentGVars
    After each function call, show globally allocated GPU variables
    that are still residing in GPU global memory; this works only if 
    "globalGMallocOpt" flag is on. (this info can be used for debugging.)
  -addSafetyCheckingCode
    Add GPU-memory-usage checking code just before each kernel call
    (Used for debugging)
  -addCudaErrorCheckingCode
    Add CUDA-error-checking code right after each kernel call.
    If this option is on, "forceSyncKernelCall" option is suppressed, 
      since the error-checking code contains a built-in synchronization
      call. 
    Used for debugging
  -OmpAnalysisOnly        
    Conduct OpenMP analysis only.
  -OmpKernelSplitOnly
    Generate kernel-split OpenMP codes without CUDA translation.

***************************
* OpenMPC User-directives *
***************************
- The following four types of user-directives can be used to annotate 
  each OpenMP parallel region.
  #pragma cuda gpurun [clause[[,] clause]...]

    where clause is one of the following
      c2gmemtr(list) 
      noc2gmemtr(list) 
      g2cmemtr(list) 
      nog2cmemtr(list)
      registerRO(list) 
      registerRW(list) 
      noregister(list)
      sharedRO(list) 
      sharedRW(list) 
      noshared(list) 
      texture(list) 
      notexture(list) 
      constant(list)
      noconstant(list)
      maxnumofblocks(nblocks)
      noreductionunroll(list)
      nocudamalloc(list)
      nocudafree(list)
      cudafree(list)
      noploopswap
      noloopcollapse
      threadblocksize(N)

  #pragma cuda cpurun [clause[[,] clause]...]

    where clause is one of the following
      c2gmemtr(list) 
      noc2gmemtr(list) 
      g2cmemtr(list) 
      nog2cmemtr(list)

  #pragma cuda ainfo procname(proc-name) kernelid(id)

  #pragma cuda nogpurun

- Brief description of OpenMPC clauses
  c2gmemtr(list)   : Set the list of variables to be transferred from CPU to GPU
  noc2gmemtr(list) : Set the list of variables not to be transferred from CPU to GPU 
  g2cmemtr(list)   : Set the list of variables to be transferred from GPU to CPU 
  nog2cmemtr(list) : Set the list of variables not to be transferred from GPU to CPU
  registerRO(list) : Cache R/O variables in the list onto GPU registers 
  registerRW(list) : Cache R/W variables in the list onto GPU registers 
  noregister(list) : Set the list of variables not to be cached on GPU registers
  sharedRO(list)   : Cache R/O variables in the list onto GPU shared memory 
  sharedRW(list)   : Cache R/W variables in the list onto GPU shared memory 
  noshared(list)   : Set the list of variables not to be cached on GPU shared memory
  texture(list)    : Cache variables in the list onto GPU texture memory 
  notexture(list)  : Set the list of variables not to be cached on GPU texture memory 
  maxnumofblocks(nblocks) : Set maximum number of CUDA thread blocks for a kernel
  threadblocksize(N)      : Set CUDA thread block size for a kernel
  noreductionunroll(list) : Do not apply loop unrolling for in-block reduction
  nocudamalloc(list)      : Set the list of variables not to be CUDA-mallocated
  nocudafree(list)        : Set the list of variables not to be CUDA-freed
  cudafree(list)          : Set the list of variables to be CUDA-freed
  noploopswap             : Do not apply Parallel Loop-Swap optimization
  noloopcollapse          : Do not apply Loop Collapse optimization

- Usage of OpenMPC clauses
  "registerRO" may contain
     R/O shared scalar variables
     R/O shared array element (ex: a[i])
  "registerRW" may contain
     R/W shared scalar variables
     R/W shared array element (ex: a[i])
  "noregister" may contain
    R/O or R/W shared scalar variables
    R/O or R/W shared array element (ex: a[i])
  "sharedRO" may contain
    R/O shared scalar variables
    R/O private array variables
  "sharedRW" may contain
    R/W shared scalar variables (not yet implemented)
    R/W private array variables
  "noshared" may contain
    R/O or R/W shared scalar variables
    R/O or R/W private array variables
  "texture" may contain
    R/O 1-dimensional shared array
"notexture" may contain
    R/O 1-dimensional shared array

**********************************************
* Optimization Search Space Setup For Tuning *
**********************************************
- If "genTuningConfFiles" flag is used, the O2G translator 1) analyzes 
  an input program and finds all applicable tuning parameters, which consist of 
  O2G compiler flags and OpenMPC clauses, and 2) generates a set of tuning 
  configuration files and/or user-directive files for searching the given 
  parameter space. (Each configuration file and user-directive file can be
  fed to the translator using "cudaConfFile" and "cudaUserDirectiveFile" flags.)
  A user can further restrict the search space using 
  optimization-space-setup file ( "defaultTuningConfFile" flag is used to 
  specify the setup file.)

- The optimization-space-setup file has the following format:
  defaultGOptionSet(list)
    where list is a comma-separated list of O2G compiler flags that will
    be always applied.

  excludedGOptionSet(list)
    where list is a comma-separated list of O2G compiler flags that will
    not be applied.

    In the above two types, the following flags can be used.
      assumeNonZeroTripLoops
      useGlobalGMalloc
      globalGMallocOpt
      cudaMallocOptLevel     
      cudaMemTrOptLevel
      useMatrixTranspose
      useMallocPitch
      useLoopCollapse
      useParallelLoopSwap
      useUnrollingOnReduction
      shrdSclrCachingOnReg
      shrdArryElmtCachingOnReg
      shrdSclrCachingOnSM
      prvtArryCachingOnSM
      shrdArryCachingOnTM
      cudaThreadBlockSize
      maxNumOfCudaThreadBlocks
    For "defaultGOptionSet", additional, following flags can be used.
      disableCritical2ReductionConv
      UEPRemovalOptLevel
      forceSyncKernelCall
      doNotRemoveUnusedSymbols

  cudaMemTrOptLevel=N

  cudaMallocOptLevel=N
  
  UEPRemovalOptLevel=N

  cudaThreadBlockSet(list)
    where list is a comma-separated list of numbers

  maxnumofblocksSet(list)
    where list is a comma-separated list of numbers

************************
* Typical tuning steps *
************************
- OpenMPC does not have a built-in tuning system, but contains building blocks 
to be used to create a custom tuning system. Typical tuning steps will be the 
following:
	1) For a given input OpenMP program, compile it with "-extractTuningParameters"
	option, which will generate a set of tuning parameters applicable to the input program.
	2) Create an "optimization-search-space-setup" file using the parameters found in the
	previous step. Detailed format of the setup file is explained in the previous section
	("Optimization Search Space Setup For Tuning" section)
	3) Create a set of tuning configuration files and/or user-directive files. For this,
	compile the input program again with the following options:
		-genTuningConfFiles=<tuning-dir> -defaultTuningConfFile=<search-setup-file> 
		// <tuning-dir> is the place where the output tuning configuration files are stored.
		// <search-setup-file> is the one created in the step 2.
		// By default, configuration files for program-level tuning are generated.
		// To generate configuration files for kernel-level tuning, add "-tuningLevel=2"
		// option to the above options.
	4) Perform tuning tests by compiling the input program using the generated configuration
	files and measuring the performance of the compiled program. Tuning steps for
	a simple, exhausive search will be the following: 
		For each tuning configuration file (ex: confFile.txt),
			a) compile the input program with the tuning configuration file
				$ java omp2gpu.exec.OMP2GPUDriver -cudaConfFile=confFile.txt *.c
			b) Execute the compiled program and measure performance

**********************
* Additional feature *
**********************
- O2G translator accepts OpenMP "collapse" clauses, which can be used
  to collapse nested, parallel loops into a single loop with bigger 
  iteration size.  
  

REQUIREMENTS
------------
Refer to readme file for Cetus.


INSTALLATION
------------
Refer to readme file for Cetus.


RUNNING OpenMP-to-GPU TRANSLATOR
--------------------------------
Users can run the O2G translator in the following ways:

  $ java -classpath <user_class_path> omp2gpu.exec.OMP2GPUDriver <options> <C files>

The "user_class_path" should include the class paths of Antlr and Cetus.

Alternatively you can also run the OMP2GPU.sh script to create the cetus wrapper script
inside the "bin" folder.

LIMITATIONS
-----------
- Current O2G translator generates CUDA code compatible with NVCC version 1.0.
  - NVCC V1.0 does not support inlining of external functions, and thus 
    all device functions called in a kernel function should be in the same
    file where they are called.
	- Necessary inlining should be done by a user manually.

- OpenMP shared variables accessed in a kernel region (an OpenMP parallel region 
  to be transformed into a kernel function) should be either scalar or array types.
  - OpenMP shared array variables accessed in the kernel region should have all 
    dimension information, which is needed for O2G translator to allocate GPU memory 
    for the shared array variables.
    - For example, in the following function, if the function parameter, a, is an OpenMP
    shared variable, it may not be handled by the current O2G translator.
    
      void func( float a[] ) { ... }  

	  //Correct form is the following:
	  void func( float a[SIZE] ) { ... }

	- To convert a pointer to multi-dimensional array (ex: double pointer), refer to the 
	following example:

	  //Original code
	  void comp(float **a) {
		  int i,m; 
		  #pragma omp parallel for shared(a) private(i,m)
		  for(i=0; i<SIZE1; i++) 
			  for(m=0; m<SIZE2; m++)
				  a[i][m] = i+m;
	  }
	  int main(int argc, char *argv[]) {
		  int i; float **a;
		  a = (float **)malloc(SIZE1 * sizeof(float *));
		  for(i=0; i<SIZE1; i++) 
			  a[i] = (float *)malloc(SIZE2 * sizeof(float));
		  ...
		  comp(a);
		  ...
	  }

	  //Modified code
	  void comp(float a[SIZE1][SIZE2]) {
		  int i,m; 
		  #pragma omp parallel for shared(a) private(i,m)
		  for(i=0; i<SIZE1; i++) 
			  for(m=0; m<SIZE2; m++)
				  a[i][m] = i+m;
	  }
	  int main(int argc, char *argv[]) {
		  int i; float (*a)[SIZE2];
		  a = (float (*)[SIZE2])malloc(SIZE1 * SIZE2 * sizeof(float));
		  ...
		  comp(a);
		  ...
	  }


- Current reduction-transformation pass can not handle the cases where
  reduction is used in a function called in a parallel region.

- To enforce a global synchronization in the CUDA model, an OpenMP parallel region
  is split into two sub-regions at each of OpenMP synchronization constructs, but
  this splitting may break correctness by resulting in upwardly exposed private 
  variables. To check this problem, UEPrivateAnalysis is invoked at the end of 
  O2G translation, and if a problem is detected, a programmer should fix it
  by modifying an input OpenMP program manually.
	Here are some suggestions:
  1) Try a built-in UEP Removal optimization (UEPRemovalOptLevel=N,  
     where N = 1, 2, or 3). Because this optimization may be unsafe,  
     the programmer should verify the correctness manually.          
  2) Remove unnecessary synchronizations such as removing unnecessary
     barriers or adding nowait clause to omp-for-loop annotations if 
     applicable.                                                      
  3) If all participating threads have the same value for the private
     variable,                                                        
     - Either add firstprivate OpenMP clause to related omp parallel 
       regions.                                                     
     - Or if the first method is not applicable, change the private
       variable to  shared variable, and enclose statements that     
       write the variable with omp single directive.                
  CF: Due to the inaccuray of a compiler analysis, the following warning
  may be false ones. Here are some reasons causing false warning:  
  1) Current UEPrivateAnalysis conducts an intraprocedural analysis;
     if a function is called inside of the target procedure, the    
     analysis conservatively assumes all variables of interest are 
     accessed in the called function, and thus analysis may result in 
     overly estimated, false outputs.                                
  2) Private arrays can be falsely included to the UEUSE set; even if
     they are initialized in for-loops, compiler may not be sure of 
    their initialization due to the possibility of zero-trip loops.
  3) Current UEPrivateAnalysis handles scalar and array expressions,
     but not pointer expressions. Therefore, if a memory region is  
     accessed both by a pointer expression and by an array expression,
     the analysis may not be able to return accurate results.       


REFERENCE
---------
- To cite this work, use the following papers:

[1] Seyong Lee and Rudolf Eigenmann, "OpenMPC: Extended OpenMP Programming and 
  Tuning for GPUs", in SC'10: Proceedings of the 2010 ACM/IEEE conference on 
  Supercomputing. New Orleans, Louisiana, USA: IEEE Press, 2010.

  BibTeX:

@inproceedings{LeeSC10,
  author = {Seyong Lee and Rudolf Eigenmann},
  title = {OpenMPC: Extended OpenMP Programming and Tuning for GPUs},
  booktitle = {SC'10: Proceedings of the 2010 ACM/IEEE conference on Supercomputing},
  publisher = {IEEE press},
  year = {2010},
  pages = {}
}

[2] Seyong Lee, Seung-Jai Min, and Rudolf Eigenmann, "OpenMP to GPGPU: A compiler
  framework for automatic translation and optimization", in PPoPP '09: Proceedings
  of the 14th ACM SIGPLAN symposium on Principles and practice of parallel programming.
  New York, NY, USA: ACM, Feb. 2009, pp. 101-110.

  BibTeX:

@inproceedings{LeePPOPP09,
  author = {Seyong Lee and Seung-Jai Min and Rudolf Eigenmann},
  title = {OpenMP to GPGPU: A Compiler Framework for Automatic Translation and Optimization},
  booktitle = {PPoPP '09: Proceedings of the 14th ACM SIGPLAN symposium on Principles and practice of parallel programming},
  publisher = {ACM},
  year = {2009},
  pages = {101--110}
}




