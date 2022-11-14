
#include <stdio.h>
#include <omp.h>

int main(int argc, char const *argv[])
{

    int n, m;

    if (argc == 0)
    {
        n = 20;
    }

    if (argc > 0)
    {
        n = argv[0];
    }

    m = n;

    if (argc > 1)
    {
        m = argv[1];
    }

    float a[n][n], b[n][m], d[n][m];

    int i, j, k, kk, jj, ii;

    int kTile = 4096;
    int jTile = 4096;
    int iTile = 4096;


    //One could parallelize here but a reeduction is required
    for (ii = 0; ii < n; ii += iTile)
    {
        //to avoid race condition I parallel i loop to avoid several threads accessing
        //the same indexes
        #pragma omp parallel for private(ii, i, jj, kk, j, k)
        for (i = 0; i + iTile - 1 < n ? iTile - 1 + i : n; i++)
        {
            for (jj = 0; jj < n; jj += jTile)
            {

                for (kk = 0; kk < n; kk += kTile)
                {

                    for (j = jTile; j + jTile - 1 < n ? jTile - 1 + j : n; j++)
                    {

                        for (k = kk; k + kTile - 1 < n ? kTile - 1 + k : n; k++)
                        {

                            d[i][j] = d[i][j] + a[i][k] * b[k][j];
                        }
                    }
                }
            }
        }
    }

    return 0;
}
