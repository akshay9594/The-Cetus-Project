/*  
 Very Simple Parallelizable Loop Example
*/

#define n 10000
int initialize(int a[], int m);

int main(){
    
  int a[30000];
  
  int b[30000];

  int c[30000];
  
  initialize(b, n);
  for (int i=0 ; i<n; i++) {

   for(int j=0; j<n; j++){
      a[2*n*i+j]= b[i];
      c[i] = a[2*n*i+j+n];
   }

  }


}

int initialize(int a[], int m){

  for(int i=0; i<m; i++){
     a[i] = i;
  }

  return a;

}
