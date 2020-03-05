/*  Very Simple Parallelizable Loop Example
*/

int main(){

  double a[30000];
  
  int b[30000];
  
  int i ,n ,k;

  int j;

  for ( i=1 ; i<30000; i++) {

    a[i]= b[i];

  }


  for ( j=1 ; j<30000; j++) {

    b[j]= j * 3;

  }

   for ( i=1 ; i<10000; i++) {

     for(k = b[i] ; k < b[i+1] ;k++){

        a[k]= k - 1;
    }
   }


	
   return 0;
}
