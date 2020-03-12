


/* Scalar Reduction

  The loop contains a scalar reduction operation.

*/

#define  MAX(X,Y)  (((X) > (Y)) ? (X) : (Y))

int main(){

    int a[10000] , c[10000];
    
    int b = 1 ,i , maxl = 1 , minl = 1000 , d , e;

    for( i = 0 ; i < 10000 ;i++){

        b &= a[i];
        d += a[i];
        e *= a[i];

    }



    for( i = 0 ; i < 10000 ;i++){

       maxl = MAX(maxl , a[i]);
    
    }




      for( i = 0 ; i < 10000 ;i++){

        if (a[i] > maxl) 
          maxl = a[i]; 

      //c[i] = minl;
    
    }


      for( i = 0 ; i < 10000 ;i++){

       minl = (minl < a[i]) ? minl : a[i];
    
    }


	
   return 0;
}



