/*  Non-profitable Parallel Loop:
     This loop could be parallelized but has only a small number of iterations.
     With 1000 iterations, the sequantial loop executes in less than 
      
*/

#define M 1000
#define N 1000

int main()
{

  float corr[M][N], data[M][N];
  int i,j,k;
  
   for (i = 0; i < M-1; i++)
    {
      corr[i][i] = 1.0;
      for (j = i+1; j < M; j++)
        {
          corr[i][j] = 0.0;
          for (k = 0; k < N; k++){
            corr[i][j] += (data[k][i] * data[k][j]);
          }
          corr[j][i] = corr[i][j];

        }
  
     }
  
   return 0;
}
