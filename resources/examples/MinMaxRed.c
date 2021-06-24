

/*

Loops to test MIN and MAX Reductions.
Also loop to test multiple reduction statements with different operators.
MIN and MAX aren't intrinsic functions in C/C++.

*/

#include <stdio.h>
#include <math.h>

#define  MAX(X,Y)  (((X) > (Y)) ? (X) : (Y))

int main(){

    int a[10000] , c[10000];

    
    int b = 1 ,i , maxl = 1 , minl = 1000 , d , e;

    int x1,x2,t1,t2,t3,t4,l,sx,sy;

    for ( i = 0 ; i < 10000 ;i++){

        b &= a[i];
        d += a[i];
        e *= a[i];
        maxl = MAX(maxl , a[i]);

    }



      for ( int j = 0 ; j < 10000 ;j++){

        if (a[j] > maxl) 
          maxl = a[j]; 

      }


      for ( i = 0 ; i < 10000 ;i++){

        minl = (minl < a[i]) ? minl : a[i];
    
      }


	
   return 0;
}

