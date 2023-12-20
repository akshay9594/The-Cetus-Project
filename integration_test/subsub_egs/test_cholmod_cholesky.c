/*  
Subscripted subscript example from CHOLMOD SuiteSparse 5.4.0
*/

// #include <stdio.h>
// #include <math.h>
// #include <stdlib.h>

#define N 30000

int main(){

int i,j,k, p, q , k1, k2;

int pf, psx, nsrow, psi, Ls[N];

int fjk[2], Ax[N], Ap[N], Ai[N], Lx[N], Fx[N], Map[N], Fi[N], Fp[N];

int stype;

stype = 0;


for(i = 0; i < nsrow; i++)
{
   Map[i] = -1;
} 

for(k = 0; k < nsrow; k++)
{
    Map[Ls[psi + k]] = k;
}


//Loop to parallelize
  for (k = k1 ; k < k2 ; k++)
   {
       if (stype != 0)
      {
         for (p = Ap[k]; p < Ap [k+1]; p++)
         {
               /* row i of L is located in row Map [i] of s */
               i = Ai [p] ;
               if (i >= k)
               {
                        
                  if(Map[i] >= 0 && Map [i] < nsrow)
                  {
                     Lx[(Map[i]+(psx+(k-k1)*nsrow))] = Ax[p];
                                  
                  }
               }
         }
      }
      else
      {
          /* copy the kth column of A*F into the supernode */
                        
         for (pf = Fp[k]; pf < Fp[k+1]; pf++)
         {

               for (p = Ap[Fi[pf]]; p < Ap[Fi[pf]+1] ; p++)
               {
                  i = Ai [p] ;
                  if (i >= k)
                  {
                     /* See the discussion of imap above. */
                     if (Map[i] >= 0 && Map[i] < nsrow)
                     {
                        /* Lx [Map [i] + pk] += Ax [p] * fjk ; */
                        Lx [(Map[i]+psx+(k-k1)*nsrow)] += Ax [p] * Fx[pf];

                     }
                  }
               }
         }
      }

   }

 
  
	
   return 0;
}
