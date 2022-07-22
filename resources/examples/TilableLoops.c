#include <stdio.h>
#include <math.h>

int sum(int a, int b)
{
    return a + b;
}

int main()
{

    int n = 30000;

    int i, j;
    int a[n], b[n], c[n];

    // Tilable loop, perfect nest
    for (i = 0; i < n; i++)
    {
        for (j = 0; j < n; j++)
        {
            a[n] = 10;
        }
    }

    // Non tilable loop. Non perfect nest
    for (i = 0; i < n; i++)
    {
        b[n] = 10;
        for (j = 0; j < n; j++)
        {
            c[n] = 10;
        }
    }

    // Non tilable loop. Function call
    for (i = 0; i < n; i++)
    {
        for (j = 0; j < n; j++)
        {
            c[n] = sum(a[i], b[j]);
        }
    }

    // Non tilable loop : Non canonical
    for (i = 0;;)
    {
        for (j = 0; j < n; j++)
        {
            c[n] = a[i] + b[i];
        }
    }

    // Tilable Or not? innermost loop is non canonical (and infinite)
    // Take into account that the idea is to parallelize the outermost
    for (i = 0; i < n; i++)
    {
        for (j = 0;;)
        {
            c[n] = a[i] + b[j];
        }
    }

    // Tilable loop
    for (i = 0; i < n; i++)
    {
        for (j = 0; j < n; j++)
        {
            c[n] = a[i] + b[j];
        }
    }

    return 0;
}
