
#include <stdio.h>

int main(int argc, char const *argv[])
{

    int n, m;

    int a[n][n], b[n][m], d[n][m];

    int i, j, k;

    int c[n];

    for (int i = 0; i < n; i++)
    {
        for (int j = 0; j < n; j++)
        {
            a[i][j] = a[i*5][j] + a[i * 4][i*3];
        }
    }
    return 0;
}
