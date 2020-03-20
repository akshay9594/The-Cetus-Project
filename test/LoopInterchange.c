

#define  MAX(X,Y)  (((X) > (Y)) ? (X) : (Y))

int main(){

    int a[10000] , c[10000] , b[10000][10000] , d[10000][10000];
    
    int i ,j;

    for(i = 0 ; i < 10000 ;i++){

      for( j = 0 ; j < 10000 ;j++){

        b[j][i+1] = 2 * b[j][i-1];

      }
    }


    for(i = 0 ; i < 10000 ;i++){

      for( j = 0 ; j < 10000 ;j++){

        d[i][j+1] = 2 * d[i][j-1];

      }
    }

   

   return 0;
}

