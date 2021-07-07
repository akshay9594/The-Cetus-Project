


/* Scalar Reduction

  The loop contains a scalar reduction operation.

*/

#include <stdio.h>

int main(){

  float a[10000], sum;
  int i, n;
  n = 10000;
  
  for (i=1; i<n; i++) {
    sum = sum + a[i];
  }
	
   return 0;
}


