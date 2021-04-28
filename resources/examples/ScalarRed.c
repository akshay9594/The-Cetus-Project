


/* Scalar Reduction

  The loop contains a scalar reduction operation.

*/

int main(){

  int a[10000] , c[10000];
  
  int b = 1 ,i;

  for( i = 0 ; i < 10000 ;i++){

    b = b || a[i];
  }


	
   return 0;
}



