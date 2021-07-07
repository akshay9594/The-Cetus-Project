/* Array Reduction

  The loop contains an array reduction operation, 
  a.k.a irregular reduction or histogram reduction

*/

#include <stdio.h>
#include <math.h>

#define N 10000
#define firstcol 10

int main(){

  float a[10000], sum , rowstr[N], colidx[N];
  int i, n, tab[10000], j, k, q[N];
  
  /* define content of sum and a */

  // for (i=1; i<10000; i++) {
  //   sum[tab[i]] = sum[tab[i]] + a[i];
  // }
	
   for (j = 0; j < 100000; j++) {
      sum = 0.0;
      for (k = rowstr[j]; k < rowstr[j+1]; k++) {
        sum = sum + a[k];
      }
      q[j] = sum;
    }
   return 0;
}
