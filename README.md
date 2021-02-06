# The Cetus Project
Cetus Source to Source compiler improvements being done at University of Delaware

## Install Instructions:
        1. Download Cetus through the "Download Code" (green button) above or through wget.
        2. Unpack the Zip/Tar file and navigate to the main directory.
        3. Run the build script through the command - ./build.sh bin
        4. The Cetus executable is created in the bin directory
        5. Copy and paste the Cetus executable in your working directory.
        6. Run the Cetus executable to see the list of available options.
        7. To compile a source code using Cetus through the command line type-
                ./cetus [options] [C FILE]
                E.g. ./cetus -parallelize-loops=2 foo.c
        8. Inside the resource directory, you can find example programs

## Bug Fixes and added features

### 1. Handling of loop index initializations within FOR loop declaration-
        When for loops are declared as- for(int i = 0; i < n; i++), 
        cetus would remove the initialization of 'i' from it's place and 
        hoist it at the top of the code block.
        The reason for doing so would be the use of underlying cetus principle that- 
        "All variable declarations should appear at the top of the code block". 
        Cetus wouldn't parallelize a loop without knowing the initial value of the loop index.
        Fixed this so that instead of having a loop like-  for( ;i < n; i++),
        We can now have  - for(i = 0 ; i < n; i++) and 'int i' would appear at the top of the block.
    
### 2. Added support for logical and bitwise scalar reductions
        Scalar reductions of the form x = x op expr, where op is any of:
        {logical AND - && , logical OR- || , Bitwise OR - | , Bitwise AND- & , Bitwise XOR - ^}. 
        Also added support for Bitwise assignment operators of the form: 
        {Bitwise AND - &= , Bitwise OR - |= , Bitwise XOR - ^=}
        
### 3. Support for Min and Max Reductions
       OpenMP added the reduction-identifiers "min" and "max" to the reduction clause from OpenMP 3.1.
       Cetus can now recognize Min and Max reductions implemented using an if-statement or using the
       Conditional operator (?). Some expression restrictions apply.
       
### 4. Support for Multiple reductions using different operators
       Cetus had no issue recognizing multiple unique reduction statements within the same loop. 
       But Cetus could not create a separate reduction clause for each reduction-identifier within 
       the same directive as per the atest OpenMP specification. The support for the same has now been added. 
       The directive would look something like:
       
       Eg. #pragma omp parallel for private(i) reduction(max: maxl)reduction(&: b)reduction(+: d)
           Earlier Cetus would try to include all the identifiers and operators within one reduction clause.
       
### 5. Loop Interchange Pass Added to Cetus
       a. Loop Interchange legality algorithm had some minor bugs which have been fixed.
       b. Reusability analysis added to the Loop Interchange pass which determines the 
          best order of loops in the nest for maximizing reusability of cache lines. 
       c. Model taken from K.S McKinley’s paper- "Optimizing for Parallelism and Data locality".
       d. Pass can also handle symbolic loop bounds.
       My contribution was everything else besides the leglity test.


    
    
  
            
