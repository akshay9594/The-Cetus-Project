/*  Very Simple Parallelizable Loop Example
*/

#include <stdio.h>
#include <math.h>

int main(){
    
  int a[30000] , int b[30000];

  for (int i=1 ; i<30000; i++) {

    a[i]= b[i];

  }

