/* 
 Subscripted subscript example from the Algebraic Multigrid kernel (Amgmk)
*/

#include <stdio.h>
#include <math.h>

#define N 30000

int main(){

   int idx=0, row_val[N], nonzeros, col_ind[N], row[N] ,row_ptr[N], n_rows;
   double nnz_val[N], p[N], W[N], H[N];
    int holder,i ,k;
    int r, ind,t;
    double sm;

    row[0]=0;
    holder=0;
    r = row_val[idx];

    for(i =0; i < nonzeros; i++){
        if(row_val[i] != r){
            row_ptr[holder++] = i;
            r = row_val[i];
        }
    }
    //row_ptr[holder+1] = nonzeros-1;

    //#pragma omp parallel for private(sm,r,ind,t)
    for (r = 0; r < n_rows; ++r){
        for (ind = row_ptr[r]; ind < row_ptr[r+1]; ++ind){
            sm=0;
            for (t = 0; t < k; ++t){
                sm += W[r * k + t] * H[col_ind[ind] * k + t];
               
            }
            p[ind] = sm * nnz_val[ind];     //Scaling of non-zero elements of the sparse matrix
           
        }                
    } 

   return 0;
}
