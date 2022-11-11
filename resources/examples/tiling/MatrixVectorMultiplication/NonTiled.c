
#include <stdio.h>

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

    int a[n*n], b[n], c[n];

    int i, j;
    
    for (i = 0; i < n; i++)
    {
        for (j = 0; j < n; j++)
        {
            c[i] += a[i * n + j] * b[j];
        }
    }

    return 0;
}
