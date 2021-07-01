

/*
* @@name:	reduction.7c
* @@type:	C
* @@compilable:	yes
* @@linkable:	no
* @@expect:	success
*/
#include <stdio.h>

#define N 100

int main(){

  int i,j;
  float a[N], b[N][N];

  for(i=0; i<N; i++) 
  {
    a[i]=0.0;
  }

  for(i=0; i<N; i++) 
  {
    for(j=0; j<N; j++) 
      b[j][j]= i;
  }


  for(i=0; i<N; i++){
    for(j=0; j<N; j++){
       a[j] +=  b[i][j];
    }
  }
  printf(" a[0] a[N-1]: %f %f\n", a[1], a[N-1]);

  return 0;
}
