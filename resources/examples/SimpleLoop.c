/*  Very Simple Parallelizable Loop Example
*/

#include <stdio.h>
#include <math.h>

int main(){
    
  int a[30000] , b[30000], n = 30000;

  for (int i=0 ; i<n; i++) {

    a[i] = b[i];

  }

}
