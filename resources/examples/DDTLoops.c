#include <stdio.h>
#include <math.h>

int main()
{

    int n = 30000;

    int i;
    int a[n];

    // LOOP A
    //  true dependence. Read after write
    //  Each processor will execute a line of this code
    //  procesor 1: i=1 a[1] (read) = a[0] ,
    //  procesor 2: i=2 a[2]=a[1] (write);
    //  but,
    //  what if processor 2 is ahead of processor 1 ? (trying to explain why dd graph shows an anti dependence here)
    //  or does it dependes on the way how statements are processed?
    //
    //  for (i = 1; i < n; i++)
    //  {
    //      a[i] = a[i - 1];
    //  }

    // int a1[n], b1[n];
    // // i=1; a1[1]=10 (write); b1[1]=a[0];
    // // i=2; a1[2]=10; b1[2]=a[1] (read);

    //  LOOP B

    // for (i = 1; i < n; i++)
    // {
    //     a1[i] = 10;
    //     b1[i] = a1[i - 1];
    // }

    // LOOP C
    int x[n], y[n];
    for (int j = 0; j < n; j++)
    {
        for (i = 1; i < n; i++)
        {
            x[i] = 10;
            y[i] = x[i - 1];
        }
    }

    // int b[n];

    // // anti dependence. Write after read
    // // i=1 b[1] = b[2] (read); i=2 b[2] (write) = b[3];
    // for (i = 1; i < n - 1; i++)
    // {
    //     b[i] = b[i + 1];
    // }

    // int c[n];
    // int d[n];

    // // true dependence
    // for (i = 1; i < n; i++)
    // {
    //     c[i] = 10;
    //     d[i] = c[i - 1];
    // }

    // // cross-loop dependence
    // int c[n];
    // int e[n];

    // for (i = 1; i < n; i++)
    // {
    //     e[i] = c[i - 1];
    // }

    return 0;
}
