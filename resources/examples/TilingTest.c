/* Compute Jacobian; Take about 5 seconds to finish on single core*/

#include <math.h>
#include <stdio.h>

// Example extracted directly from: "Optimizing for Parallelism and Data Locality",
// Kathryn S. McKinley

int main()
{
    int n = 100000;
    int i, j;
    float a[n][n], b[n][n], arr[n];

    for (int i = 0; i < n; i++)
    {
        for (int j = 0; j < n; j++)
        {
            arr[i] = arr[j] + arr[i * 5];
        }
    }

    // for (i = 1; i * 2 < n; i++)
    // {
    //     for (j = 1; j * 5 < n; j++)
    //     {
    //         a[i][j] = b[i * 2][i * 3] + b[i * 4][j * 5];
    //     }
    // }

    // for (i = 0; i < n; i++)
    // {
    //     for (j = 1; j < n; j++)
    //     {
    //         a[j][j] = a[j][j - 1] + 10;
    //     }
    // }

    // for (i = 1; i < n; i++)
    // {
    //     for (j = 0; j < n; j++)
    //     {
    //         a[i][i] = a[i][i - 1] + 20;
    //     }
    // }

    return 0;
}
