/* Array Privatization Example

  The variable t is an array used temporarily during a single iteration of the
  outer loop. No value of t is used in an iteration other than the one that
  produced it. Without privatization, executing different iterations in
  parallel would create conflicts on accesses to t.  Declaring t private gives
  each thread a separate storage space, avoiding these conflicts.

*/

#include <math.h>
int main(){

  float a[10000][1000], b[10000][10000], t[10000];
  int i, j;
  float c[10000], d[10000],x;
  
  for (i=1; i<10000; i++) {
     for (j=1; j<10000; j++) {
       t[j] = a[i][j]+b[i][j];
     }
     for (j=1; j<10000; j++) {
       b[i][j] =  t[j] + sqrt(t[j]);
     }
  }

   for (i=1; i<10000; i++) {
    x = c[i]+d[i];
    d[i] =  x + x*x;
  }
	
   return 0;
}
