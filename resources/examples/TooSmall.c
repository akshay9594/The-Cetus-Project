/*  Non-profitable Parallel Loop:
     This loop could be parallelized but has only a small number of iterations.
     With 1000 iterations, the sequantial loop executes in less than 
      
*/

#include <stdio.h>
#include <math.h>

int main(){

  float a[1000], b[1000];
  int i;
  
  for (i=1; i<1000; i++) {
    a[i]= b[i];
  }
	
   return 0;
}
