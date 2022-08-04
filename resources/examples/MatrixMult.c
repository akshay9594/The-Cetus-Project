
#include <stdio.h>


int main(int argc, char const *argv[])
{

    int n = 1000, m = 1000;

    int a[n][n], b[n][m], d[n][m];


    int i, j, k;
    // Matrix Multiplication kernel
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

    printf("FINISH");
    return 0;
}
