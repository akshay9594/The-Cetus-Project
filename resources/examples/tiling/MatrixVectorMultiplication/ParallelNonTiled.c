
#include <stdio.h>
#include <omp.h>

int main(int argc, char const *argv[])
{

    int n;

    if (argc == 0)
    {
        n = 20;
    }

    if (argc > 0)
    {
        n = argv[0];
    }

    float a[n*n], b[n], c[n];

    int i, j;
    #pragma omp parallel for private(i,j)
    for (i = 0; i < n; i++)
    {
        for (j = 0; j < n; j++)
        {
            c[i] += a[i * n + j] * b[j];
        }
    }

    return 0;
}
