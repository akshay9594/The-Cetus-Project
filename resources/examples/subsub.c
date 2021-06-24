

#include <stdio.h>
#include <math.h>

int main(){

 int a[30000],b[30000],c[30000], d[30000];
  
  int i , n , p , x;
  int j;
   p = 1;
   n = 3000;


for (j = 0; j < n; j++){

   d[j] = p;
   for (i = 0; i < n; i++){

      if(c[i] != 0){

         b[i] = i;
         if(x != 0){
            a[i] = c[i];
         }
         p++;
      }
   }

   
}


for (j=0 ; j < n; j++) {

     if(j%2 ==0)
       a[j] = 0;
      else
       a[j] = 1;
     
  }

  for (j=0 ; j<n; j++) {

     a[j] = a[j-1] + a[j];
     
  }
 
  
	
   return 0;
}
