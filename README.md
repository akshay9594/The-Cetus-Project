# The-Cetus-Project
Cetus Source to Source compiler improvements being done at University of Delaware

## Bug Fixes

### 1. Handling of loop index initializations within FOR loop declaration-
        When for loops are declared as- for(int i = 0; i < n; i++), 
        cetus would remove the initialization of 'i' from it's place and hoist it at the top of the code block.
        The reason for doing so would be the use of underlying cetus principle that- 
        "All variable declarations should appear at the top of the code block". 
        Cetus wouldn't parallelize a loop without knowing the initial value of the loop index.
        Fixed this so that instead of having a loop like-  for( ; i < n; i++),
        We can now have  - for(i =0 ; i < n ;i++) and 'int i' would appear at the top of the block.
    
### 2. Added support for logical and bitwise scalar reductions
        Scalar reductions of the form x = x op expr, where op is any of:
        {logical AND - && , logical OR- || , Bitwise OR - | , Biwise AND- & , Bitwise XOR - ^}. 
    

    
    
  
            
