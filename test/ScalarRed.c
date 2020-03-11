


/* Scalar Reduction

  The loop contains a scalar reduction operation.

*/

int main(){

    int a[10000] , c[10000];
    
    int b = 1 ,i , maxl = 1 , minl = 1000;

    for( i = 0 ; i < 10000 ;i++){

        b &= a[i];

    }



    for( i = 0 ; i < 10000 ;i++){

      if(maxl < a[i]){
        maxl = a[i];
      }
    
    }


    for( i = 0 ; i < 10000 ;i++){

        maxl = (maxl < a[i]) ? (maxl = a[i]) : maxl;
    
    }

	
   return 0;
}



