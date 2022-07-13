#include <stdio.h>
#include <math.h>

int main()
{

    int n = 30000;
    int a[n], b[n], c[n], m[n][n];

    int i;
    // true dependence
    for (i = n; i > 0; i++)
    {

        // a[i] = a[i];
        for (int j = 0; j < n; j++)
        {
            m[i - 1][j] = m[i][j];
            m[i][j] = 10;
        }
    }

    // anti
    // for (int i = 0; i < n; i++)
    // {

    //     a[i + 1] = b[i];

    //     c[i] = a[i];
    // }

    // // True and anti dependence
    // for (int i = 1; i < n; i++)
    // {

    //     a[i] = b[i];
    //     c[i-1] = a[i-1]; //raw

    //     c[i] = a[i+1]; //war
    // }

    return 0;
}
