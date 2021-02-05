

// #include "stdio.h"
// #include<stdlib.h>
// #include "omp.h"

#define  MAX(X,Y)  (((X) > (Y)) ? (X) : (Y))

  int x = 1;

  float y = 0.0;

  int c[1000][1000];
  int myArray[10000];
  int p[1000][1000] , b[1000][1000];

int main(){

   int LX1 = 5 , idel[2400][6][6][6], ntemp , iel ;

  double a;

  int i ,j ,k;


  for ( int i=0; i<10000; i++)
  {
    a = 2.0; // Or something non-trivial justifying the parallelism...
    for ( int n = 0; n<10000; n++)
    {
      myArray[n] += a;
    }
  }

 

  for( i = 0 ; i < 1000; i++){

    for( j = 0 ; j < 1000; j++){

      for( k = 0 ; k < 10000; k++){

        c[i][j] += p[i][k] * b[k][j];
       c[i][j+1] += p[i][k] * b[k][j+1];

      }


    }

  }

  
  for( i = 0 ; i < 10000 ; i++){

    for( j = 0 ; j < 10000 ; j++){

      c[i][j] = c[i+1][j+1];
    }

  }


    // for (i=0; i < len; i++) {
      
    //   c[j]+=a[i]*m[i];

    //   j++;
    // }



	
   return 0;
}

