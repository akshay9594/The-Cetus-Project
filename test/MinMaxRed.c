

#define  MAX(X,Y)  (((X) > (Y)) ? (X) : (Y))



  int x = 1;

  float y = 0.0;

int main(){

    int a[10000] , c[10000] , b[10000][10000], p[10000][10000] , q[10000][10000];

    
    int m[10000] ,i , j , k, maxl = 1 , minl = 1000 , d , e , len , n;

    int x1,x2,t1,t2,t3,t4,l,sx,sy;

    for( i = 0 ; i < 10000 ;i++){


        d += a[i];
        e *= a[i];
         maxl = MAX(maxl , a[i]);

    }



      for( int j = 0 ; j < 10000 ;j++){

        if (a[j] > maxl) 
          maxl = a[j]; 

      //c[i] = minl;
    
    }


      for( i = 0 ; i < 10000 ;i++){

       minl = (minl < a[i]) ? minl : a[i];
    
    }
     

	
   return 0;
}

