/* Scalar, Additive Reduction

  The loop contains a scalar, additive reduction operation.

*/


int main(){

  float a[10000], sum ,iter;
  int i, n = 10000, b[10000] ,j , k , r[10000] , c[10000];


  for( i = 0 ; i < 10000;i++){

    a[i] = i + 1;

    if(i % 5 == 0)
    b[i] = i;

    c[i] = i*3;

  }

  
      for (i=0; i<n; i++) {
        
        sum = 0;
        for(j = b[i] ; j < b[i+1] ;j++){
          sum = sum + a[c[j]];
        }

        r[i] = sum;
      }
    
	
   return 0;
}


