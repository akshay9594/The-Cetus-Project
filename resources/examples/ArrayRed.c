/* Array Reduction

  The loop contains an array reduction operation, 
  a.k.a irregular reduction or histogram reduction

*/
#include <stdlib.h>
int main(){

  float a[1000], sum[1000], b[1000];
  int i, n, tab[1000];
  
  n = 1000;
  /* define content of sum and a */

  for (i=n; i>=0; i--) {
    sum[tab[i]] = sum[tab[i]] + a[i];
  }
	
   return 0;
}
