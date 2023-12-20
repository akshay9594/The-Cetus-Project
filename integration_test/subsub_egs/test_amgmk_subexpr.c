/* 
 Subscripted subscript example from the Algebraic Multigrid kernel (Amgmk)
*/

#include <stdio.h>
#include <math.h>

#define N 30000
#define M 30

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

    for (i = 0; i < num_rows; i++)
   {      
          for ( j=0; j<num_vectors; ++j)
          {
        
              for (jj = A_i[Arownnz[i]]; jj < A_i[Arownnz[i]+1]; jj++) 
                y_data[ j*vecstride_y +  Arownnz[i]*idxstride_y ] +=  A_data[jj] * x_data[ j*vecstride_x + A_j[jj]*idxstride_x ];
               
          }
          
   }

  //   for (i = 0; i < num_rownnz; i++)
  //  {      
  //         for ( j=0; j<N; ++j)
  //         {
        
  //             for (jj = A_i[Arownnz[i]]; jj < A_i[Arownnz[i]+1]; jj++) 
  //               y_data[ j*3 +  Arownnz[i]*N*3] +=  A_data[jj] * x_data[ j*vecstride_x + A_j[jj]*idxstride_x ];
               
  //         }
          
  //  }

   return 0;
}
