# The Cetus Project with Subscripted subscript analysis

We have developed a new analysis technique for the automatic parallelization of subsripted
subscript loops. The technique analyzes loops that define and/or modify the subscript array
and determines array properties, which is sufficient to parallelize a class of subscripted
subscripts. This repository contains the source codes of not just the actual technique but 
also of the benchmarks used to to evaluate the capabilites of the technique. The
technique has been described in detail in the listed publications below.

## Prerequisities
### Software
 - Linux (OS tested with : CentOS v7.4, Ubuntu v22.04)
 - GNU C Compiler (GCC) v4.8.5 and above
 - Python v3.8.0 and above
 - OpenMP v4.0 and above
 - gfortran

### Python packages required

1. subprocess
2. re
3. os

### Hardware
 - Machine with x86-64 processors (preferably Sky Lake and beyond)
 - ~4GB of disk space
 - Atleast 8GB of Memory

# Downloading and Running Cetus (On this branch):
```
        1. Download Cetus through the "Download Code" (green button) above or through wget.
        2. Unpack the Zip/Tar file and navigate to the main directory.
        3. Run the build script through the command - ./build.sh bin
        4. The Cetus executable is created in the bin directory
        5. Copy and paste the Cetus executable in your working directory.
        6. Run the Cetus executable to see the list of available options and how to enable them.
        7. To compile a source code using Cetus through the command line type-
                ./cetus [options] [C FILE]
                E.g. ./cetus -parallelize-loops=2 foo.c
        8. The output file after running Cetus is made available in the cetus_output folder
            in your working directory.
        9. Inside the resource directory, you can find example programs
```

       
# Subscripted subscript Analysis Pass
1. Source Code:
    The source code of the pass can be found in-
    ```
      /src/cetus/analysis/SubscriptedSubscriptAnalysis.java
    ```
2. Enabling and testing the pass:
    To enable subscripted subscript analysis on an input code simply type:
    ```
         ./cetus -subsub_analysis -normalize-loops foo.c
     ```

# Integration Testing
Examples for testing the subscripted subscript analysis pass have been placed in the
directory "subsub_egs" within "integration_test".

## Running the integration tests
   Run the python script - SubSub_integration_test.py using the command:
   ```
   python3 SubSub_integration_test.py
   ```
   The script takes user input and can perform testing on either one or all the test files.

# Benchmarks for evaluating the technique
  The benchmarks for evaluation have been placed in the "Evaluation_Benchmarks" directory.
  Following benchmarks have been included:
  
| Code  | Source | Original Source link | 
| ------------- | ------------- | ------------- |
| amgmk-v1.0  | CORAL Benchmark Codes | (https://asc.llnl.gov/coral-benchmarks)
| UA-NPB-1.0.3 | NAS Parallel Benchmarks | (https://github.com/akshay9594/SNU_NPB-1.0.3)  
| CHOLMOD | SuiteSparse | (https://github.com/DrTimothyAldenDavis/SuiteSparse)
| SDDMM (C version) | Published Paper | (https://github.com/isratnisa/SDDMM_GPU)

## Running Subscripted Subscript Analysis on the benchmarks

A python script by the name *run-cetus.py* has been provided within each benchmark source
code. Build Cetus first and then execute the script *run-cetus.py* to get the Cetus
parallel version of the codes with subscripted subscript analysis:

  ```
  $ python3 run-cetus.py
  ```
  The translated files will be available in the *cetus_output* directory.

Note:
1. For the CHOLMOD (SuiteSparse) benchmark, only the file *cholmod_super_numeric.c*
is translated. This is due to the sheer number of dependencies present in this benchmark.
*cholmod_super_numeric.c* contains the actual supernodal cholesky factorization computation.

2. Use the Makefiles provided within each benchmark to compile and execute the codes. The
Makefiles need to be modified to compile the Cetus translated version of the source codes.
Refer to the publications below or the provided links above for more details on how to 
execute the codes.

# Related publications:
1. Akshay Bhosale and Rudolf Eigenmann. 2021. On the automatic parallelization of subscripted 
   subscript patterns using array property analysis. In Proceedings of the ACM International 
   Conference on Supercomputing (ICS '21). Association for Computing Machinery, New York, NY, 
   USA, 392–403. (https://doi.org/10.1145/3447818.3460424)


    
  
            
