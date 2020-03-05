/* Scalar, Additive Reduction

  The loop contains a scalar, additive reduction operation.

*/


int main(){

  float a[10000] , max_val , sum = 0.0;
  int i, n = 10000 ;


  for( i = 0 ; i < 10000;i++){

    a[i] = i / 2;

  }

  for( i = 0 ; i < 10000 ;i++){

    sum = sum +a[i];
  }

  
      for (i=0; i<n; i++) {
        
        if( a[i] > max_val){

          max_val = a[i];
        }

      }
    
	
   return 0;
}


