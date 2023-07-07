/*  
Subscripted subscript example from EVSL (Eigen Value Solver)
*/

#include <stdio.h>
#include <math.h>
#include <stdlib.h>

#define N 30000

int main(){

int i,j, msteps, width, sigma2, numPlaced, npts = N;

double t, ritzVal[N];

int y[N], ind[N], xdos[N], gamma2[N];

numPlaced = 0;
      
      for (j = 0; j < npts; j++) {
        if ((xdos[j] - t) < width) ind[numPlaced++] = j;
      }

      //Loop to parallelize

      for (j = 0; j < numPlaced; j++)
        y[ind[j]] += gamma2[i] *
                     exp(-((xdos[ind[j]] - t) * (xdos[ind[j]] - t)) / sigma2);

 
  
	
   return 0;
}
