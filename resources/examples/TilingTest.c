/* Compute Jacobian; Take about 5 seconds to finish on single core*/

#include <math.h>
#include <stdio.h>

// Example extracted directly from: "Optimizing for Parallelism and Data Locality",
// Kathryn S. McKinley

int main()
{
    int n = 100000;
    int i, j;
    float a[n][n], b[n][n];

    for (i = 1; i * 2 < n; i++)
    {
        for (j = 1; j * 5 < n; j++)
        {
            a[i][j] = b[i * 2][i * 3] + b[i * 4][j * 5];
        }
    }

    return 0;
}
