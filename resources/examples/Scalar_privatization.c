/* Scalar Privatization Example

  The variable t is used temporarily during a single loop iteration. No value
  of t is used in an iteration other than the one that produced it. Without
  privatization, executing different iterations in parallel would create
  conflicts on accesses to t.  Declaring t private gives each thread a separate
  storage space, avoiding these conflicts.

*/

#include <stdio.h>
#include <math.h>
int main(){

  float a[10000], b[10000], t;
  int i, n;
  
  n = 10000;
 
  for (i=1; i<n; i++) {
    t = a[i]+b[i];
    b[i] =  t + t*t;
  }
	
   return 0;
}
