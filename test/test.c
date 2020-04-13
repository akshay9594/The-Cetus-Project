

#include "stdio.h"
#include <omp.h>
#include "stdlib.h"



#include <stdio.h>
#include <sys/time.h>


#define r 10000
#define c 10000

int d[r][c];
int a[r][c];
int b[r][c];
int x[r][c];
int y[r][c];
int f[r][c];
int e[r][c];
int work[r][c][3];
int coef2[r][c] , coef4[r][c];

int main(){

   struct timeval start, end;
    int i , j , k , ld1 =1  , ld2 =1  , ldi =1  ,ld =1;

   
    for(i = 0 ; i < r ;i++){

        for(j = 0 ; j < c ; j++){

            a[i][j] = j + 1;

            b[i][j] = j + 3;

            x[i][j] = j + 2;

            y[i][j] = j + 4;

            d[i][j] = j;

            e[i][j] = j;

            f[i][j] = j + 6;

        }
    }

  


    gettimeofday(&start, NULL);

    //    for(j = 8 ; j < c ; j++){

    //       #pragma omp parallel for private( k, ld, ld1, ld2, ldi)

    //       for(k = 0 ; k < r ; k++){

    //           ld2 = a[j][k];
    //           ld1 = b[j][k] - ld2 * x[j-2][k];
    //           ld =  d[j][k] - (ld2 * y[j-2][k] + ld1 * x[j-2][k]);
    //           ldi = 1.0/ld;
    //           f[j][k] = (f[j][k] - ld2 * f[j-2][k] - ld1 * f[j-1][k]) * ldi;
    //           x[j][k] = (d[j][k] - ld1 * y[j-1][k]) * ld1;
    //           y[j][k] = e[j][k]* ldi;
    //       }
    // }

    //Taken from ARC2D (Perfect Benchmarks)
   
      for(j = 0 ; j < 15000 ;j++){

          for(k = 0 ; k < 15000; k++){

            work[j][k][3] = coef2[j][k] * work[j][k][1] - coef4[j][k] * work[j][k][2];

          }
      }


    
    //   for(k= 0 ; k < 1800 ; k++){

    //     for( i = 0 ; i < 1800; i++){

    //       for( j = 0 ; j < 1800; j++){

    //           d[i][j] = d[i][j] + a[i][k] * b[k][j];

    //       }

    //   }

    // }

    gettimeofday(&end, NULL);
 
    double time_taken; 
  
    time_taken = (end.tv_sec - start.tv_sec) * 1e6; 
    time_taken = (time_taken + (end.tv_usec -  
                              start.tv_usec)) * 1e-6; 
 
    printf("Time elpased is %f seconds \n", time_taken);


   
}