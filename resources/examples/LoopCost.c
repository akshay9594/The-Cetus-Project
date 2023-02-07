/* Compute Jacobian; Take about 5 seconds to finish on single core*/

#include <math.h>
#include <stdio.h>

#define SIZE 1024

float a[SIZE + 2][SIZE + 2];
float b[SIZE + 2][SIZE + 2];

// Example extracted directly from: "Optimizing for Parallelism and Data Locality",
// Kathryn S. McKinley

int main()
{
    int n = 100000;
    int i, j;
    float a[n][n], b[n][n];

    for (i = 1; i < n - 1; i++)
    {
        for (j = 1; j < n - 1; j++)
        {
            a[i][j] = 0.2 * (b[j][i] + b[j - 1][i] + b[j][i - 1] + b[j + 1][i] + b[j][i + 1]);
        }
    }

    return 0;
}
