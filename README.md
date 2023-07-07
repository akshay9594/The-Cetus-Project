# The Cetus Project with Subscripted subscript analysis

We have developed a new analysis technique for the automatic parallelization of subsripted
subscript loops. The technique analyzes loops that define and/or modify the subscript array
and determines array properties, which is sufficient to parallelize a class of subscripted
subscripts. This repository contains the source codes of not just the actual technique but 
also of the benchmark codes used to to evaluate the capabilites of the technique. The
technique has been described in detail in the listed publications.

# Related publications:
1. Akshay Bhosale and Rudolf Eigenmann. 2021. On the automatic parallelization of subscripted 
   subscript patterns using array property analysis. In Proceedings of the ACM International 
   Conference on Supercomputing (ICS '21). Association for Computing Machinery, New York, NY, 
   USA, 392–403. (https://doi.org/10.1145/3447818.3460424)


# Downloading and Running Cetus:
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


    
    
  
            
