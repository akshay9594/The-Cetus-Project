
/*  
  Sparse Matrix Scaling example from SuiteSparse 5.4.0
*/

#include <stdio.h>
#include <math.h>
#include <stdlib.h>

#define N 30000

int main(){
  
  int a[N] , b[N], c[N], d[N];
  int Ax[N] , s[N], Ai[N];
  
  int i , n , p , x;
  int j,k;
   p = 1;
   n = 30000;
  
  for(i=0; i<n; i++){
    b[i] =0;
  }

  for(j=0; j<n; j++){
    d[j] = p;

    for(i=0; i<n; i++){
      if(c[i]!=0){
        b[i] = i;
        if(x!=0){
          a[i] = c[i];
        }
        p++;
      }

    }

  }
  

//Loop to parallelize
  for(j=0;j<n;j++){
    
    for(k=d[j];k<d[j+1];k++){

        Ax[k] = s[j]*Ax[k]*s[Ai[k]];

    }

  }


  return 0;


}
