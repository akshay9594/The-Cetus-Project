/* Array Reduction

  The loop contains an array reduction operation, 
  a.k.a irregular reduction or histogram reduction

*/
#include <stdlib.h>
#include <stdio.h>

#define N 1000
int main(){

  float a[N];
   float sum[N], x[N] = {0.0};
  int i, n, tab[N],idx[N];
  
  /* define content of sum and a */

  for (i = 0; i < N; i++)
  {
      a[i] = i;
      tab[i] = i;
      x[i] = 0.0;
  }
  idx[0] = 3;
  idx[1] = 2;
  idx[2] = 0;
  idx[3] = 1;

  for (i = 4; i < N; i++)
  {
    idx[i] = i;
  }
  
    for (i=1; i<N; i++) {
        sum[tab[i]] = sum[tab[i]] + a[i];
        x[idx[i]] = x[idx[i]] + a[i];
    }


   return 0;
}
