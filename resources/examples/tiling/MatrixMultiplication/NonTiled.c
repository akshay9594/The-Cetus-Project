
#include <stdio.h>


int main(int argc, char const *argv[])
{

    int n, m;

    if(argc == 0) {
        n=20;
    }

    if(argc >0) {
        n=argv[0];        
    }
    
    m=n;

    if(argc >1) {
        m=argv[1];
    }


    float a[n][n], b[n][m], d[n][m];


    int i, j, k;
    // Matrix Multiplication kernel
    for (i = 0; i < n; i++)
    {

        for (j = 0; j < m; j++)
        {

            for (k = 0; k < n; k++)
            {

                d[i][j] = d[i][j] + a[i][k] * b[k][j]; //(3*m*(jTile)*4)*cores <= cacheSize 

                //
            }
        }
    }

    return 0;
}
