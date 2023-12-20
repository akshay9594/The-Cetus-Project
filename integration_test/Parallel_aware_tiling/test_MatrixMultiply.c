/*  Matrix multiplication
*/

int main(){

    int n = 1000, m = 1000;

    int a[n][n], b[n][m], d[n][m];

    int i, j, k;
    for (i = 0; i < n; i++)
    {

        for (j = 0; j < m; j++)
        {

            for (k = 0; k < n; k++)
            {
                d[i][j] = d[i][j] + a[i][k] * b[k][j];
            }
        }
    }
	
   return 0;
}
