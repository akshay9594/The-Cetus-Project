
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

    int i, j, jj;

    long jTile = 4096L;

// Reduction was added: 
// in the way, the new parallel loop is jj
// it means several thread will be able
// to write values over c[i]. So, a
// reduction should be applied over it.
// NOTE: Possible to reduce array from omp 4.5
// stripmined from j and permuteed with i
#pragma omp parallel for private(jj, i, j) reduction(+:c[n])
    for (jj = 0; jj < n; jj += jTile)
    {

        for (i = 0; i < n; i++)
        {

            for (j = jj; j + jTile < n ? j + jTile : n; j++)
            {

                c[i] += a[i * n + j] * b[j];
            }
        }
    }

    return 0;
}
