/* Array Reduction

  The loop contains an array reduction operation, 
  a.k.a irregular reduction or histogram reduction

*/
#include <stdlib.h>
#include <stdio.h>

#define N 10000
int main(){

  int i1,d1,d2,d3,mm1,mm2,i2,i3,t1,t2,t3;
  mm1 = N,mm2=N;

  d1 = 2;
  d2=d1;

  double (*z)[N][N];
  double (*u)[N][N];


     for (i2 = d2; i2 <= mm2-1; i2++) {
        for (i1 = d1; i1 <= mm1-1; i1++) {
          u[2*i3-d3-1][2*i2-d2-1][2*i1-d1-1] = 
            u[2*i3-d3-1][2*i2-d2-1][2*i1-d1-1]
            + z[i3-1][i2-1][i1-1];
        }
        for (i1 = 1; i1 <= mm1-1; i1++) {
          u[2*i3-d3-1][2*i2-d2-1][2*i1-t1-1] = 
            u[2*i3-d3-1][2*i2-d2-1][2*i1-t1-1]
            + 0.5 * (z[i3-1][i2-1][i1] + z[i3-1][i2-1][i1-1]);
        }
      }

      
      for (i2 = 1; i2 <= mm2-1; i2++) {
        for (i1 = d1; i1 <= mm1-1; i1++) {
          u[2*i3-d3-1][2*i2-t2-1][2*i1-d1-1] = 
            u[2*i3-d3-1][2*i2-t2-1][2*i1-d1-1]
            + 0.5 * (z[i3-1][i2][i1-1] + z[i3-1][i2-1][i1-1]);
        }
        for (i1 = 1; i1 <= mm1-1; i1++) {
          u[2*i3-d3-1][2*i2-t2-1][2*i1-t1-1] = 
            u[2*i3-d3-1][2*i2-t2-1][2*i1-t1-1]
            + 0.25 * (z[i3-1][i2][i1] + z[i3-1][i2-1][i1]
                    + z[i3-1][i2][i1-1] + z[i3-1][i2-1][i1-1]);
        }
      }


   return 0;
}
