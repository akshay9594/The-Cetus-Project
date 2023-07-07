/* 
 Subscripted subscript example from the Algebraic Multigrid kernel (Amgmk)
*/

#include <stdio.h>
#include <math.h>

#define N 30000

int main(){

  int i,j ,jj, irownnz,m,adiag,vecstride_y, idxstride_y, vecstride_x, idxstride_x;
  int num_vectors, num_rownnz, num_rows = N;
  int A_i[N], Arownnz[N], y_data[N], A_data[N], A_j[N], x_data[N];


  irownnz = 0;
  for (i=0; i < num_rows; i++)
  {
    adiag = A_i[i+1]-A_i[i];
    if(adiag > 0) Arownnz[irownnz++] = i;
  }
    
  //Loop to parallelize
   for (i = 0; i < num_rownnz; i++)
   {
         m = Arownnz[i];

           for (jj = A_i[m]; jj < A_i[m+1]; jj++)
           {
                   j = A_j[jj];   
                y_data[m] += A_data[jj] * x_data[j];
           } 
         
    }

  // //Loop to parallelize

  //   for (i = 0; i < num_rownnz; i++)
  //  {
  //        m = Arownnz[i];      
        
  //         for ( j=0; j<num_vectors; ++j)
  //         {
        
  //             for (jj = A_i[m]; jj < A_i[m+1]; jj++) 
  //               y_data[ j*vecstride_y + m*idxstride_y ] +=  A_data[jj] * x_data[ j*vecstride_x + A_j[jj]*idxstride_x ];
               
  //         }
          
  //  }

   return 0;
}
