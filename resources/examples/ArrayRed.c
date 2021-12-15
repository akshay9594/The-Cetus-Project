/* Array Reduction

  The loop contains an array reduction operation, 
  a.k.a irregular reduction or histogram reduction

*/
#include <stdlib.h>
int main(){

  float a[10000], sum[10000], b[10000],sum1;
  int i, n, tab[10000];
  
  n = 10000;
  /* define content of sum and a */

  for (i=n; i>=0; i--) {
    sum[tab[i]] = sum[tab[i]] + a[i];
  }

   for (i=1; i<n; i++) {
    sum1 = sum1 + a[i];
  }
	
   return 0;
}
