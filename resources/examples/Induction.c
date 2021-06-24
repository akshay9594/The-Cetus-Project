/* Examples of Loops with Induction Variables

  The first loop includes a basic, linear induction variable ind.  

  The second loop includes a more generalized induction variable, which uses a
  linear induction variable as the increment.

*/
#include <stdio.h>
#include <math.h>

int main(){

  float a[10000], b[10000];
  int i, n, ind, ind2;
  
  n = 10000;
  ind = 123;
  for (i=1; i<n; i++) {
    ind = ind + 2;
    a[ind] = b[i];
  }

  ind2 = 5;
  ind = 234;
  for (i=1; i<n; i++) {
    ind = ind + 2;
    ind2 = ind2 + ind;
    a[ind2] = b[i];
  }
	
   return 0;
}
