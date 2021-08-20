
  
/*

Different loops to test the Loop Interchange pass in Cetus.

*/


#include <stdio.h>
#include <stdlib.h>

  int a[10000][10000] , c[10000] , b[10000][10000] ,d[10000][10000] ;
    int work[10000][10000][10000], coef2[1000][10000],coef4[10000][10000];
    int S[10000] , x[10000][10000] , y[10000][10000] , f[10000][10000],e[10000][10000];
    int t[10000];

int main(){


    int n = 10000 , r = 1000 , jmi , ld1 , ld2 , ldi ,ld, m = 10000 ;

    int i,j,k,l;
    for ( i = 0 ; i < 10000 ;i++){

      for ( j = 0 ; j < 10000 ;j++){

        b[j][i] = 2 * b[j+1][i-1];

      }
    }


    
   
    // // //Taken from ARC2D (Perfect Benchmarks)
      for (k = 0 ; k < 10000 ;k++){

          for (j = 0 ; j < 10000; j++){

            work[j][k][3] = coef2[j][k] * work[j][k][1] - coef4[j][k] * work[j][k][2];

          }
      }

    
    // // From ARC2D Perfect benchmarks
       
    for (k = 0 ; k < n ;k++){

          for (j = 0 ; j < n ; j++){

              ld2 = a[j][k];
              ld1 = b[j][k] - ld2 * x[j-2][k];
              ld =  d[j][k] - (ld2 * y[j-2][k] + ld1 * x[j-2][k]);
              ldi = 1.0/ld;
              f[j][k] = (f[j][k] - ld2 * f[j-2][k] - ld1 * f[j-1][k]) * ldi;
              x[j][k] = (d[j][k] - ld1 * y[j-1][k]) * ld1;
              y[j][k] = e[j][k]* ldi;
          }
    }
      

   
  // Matrix Multiplication kernel
     for (i= 0 ; i < n ; i++){

        for ( j = 0 ; j < m; j++){

          for ( k = 0 ; k < n; k++){

              d[i][j] = d[i][j] + a[i][k] * b[k][j];


          }

      }

    }


    for ( i = 0 ; i < n; i++){

      for ( j = 0 ; j < n ;j++){

       a[j][i] = 0.2 * (b[j][i] + b[j-1][i] + b[j][i-1] + b[j+1][i] + b[j][i+1]);
      }
    }

   
   

   return 0;
}

