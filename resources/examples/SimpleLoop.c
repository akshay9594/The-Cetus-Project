/*  Very Simple Parallelizable Loop Example
*/

#include <stdio.h>
#include <math.h>

int main(){
    
  float a[30000];
  
  int b[30000];

  for (int i=1 ; i<30000; i++) {

    a[i]= b[i];

  }

  for (int k=1 ; k<30000; k++) {

    a[k]= k;

  }

   return 0;
}
