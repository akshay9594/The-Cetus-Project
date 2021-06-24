/* Compute Jacobian; Take about 5 seconds to finish on single core*/


#include <math.h>
#include <stdio.h>


#define ITER 200 //you can change this value to control the length of running time, 1000 = 30 seconds
#define SIZE 1024

float a[SIZE+2][SIZE+2];
float b[SIZE+2][SIZE+2]; 

int main(int argc, char *argv[])
{
  int i, j, k;
  double my_time;

  /*printf("JACOBI OMP VERSION: MATRIX = %d x %d, ITERATION = %d\n", SIZE, SIZE, ITER);*/

  for (i=0; i<SIZE+2; i++) {
	for (j=0; j<SIZE+2; j++) {
	  a[i][j] = 0;
	  b[i][j] = 0;
	}
  }

  /* left and right boundary initialization */
  for (j=0; j<SIZE+2; j++) {
	b[j][0] = 1.0;
	b[j][SIZE+1] = 1.0;
  }

  /* upper and lower boundary initialization */
  for (i=0; i<SIZE+2; i++) {
	b[0][i] = 1.0;
	b[SIZE+1][i] = 1.0;
  }

  /*
   *-- Timing starts before the main loop --
   */


  for (k=0; k<ITER; k++) {

	for (i=1; i<SIZE+1; i++) { 
	  for (j=1; j<SIZE+1; j++) {
		a[i][j] = (b[i-1][j] + b[i+1][j] + b[i][j-1] + b[i][j+1]) / 4;
	  }
	}

	for (i=1; i<SIZE+1; i++) { 
	  for (j=1; j<SIZE+1; j++) {
		b[i][j] = a[i][j];
	  }
	}
  }


  return 0;
}
