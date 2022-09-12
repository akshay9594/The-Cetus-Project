#line 1 "noReuseLoop.c"

#pragma startinclude #include <stdio.h>
#line 2
#include <stdio.h>
#pragma endinclude
#line 3

int main(int argc, char const *argv[])
{

    int n, m;

    int a[n];

    int i, j;

    int c[n];

    for (int i = 0; i < n; i++)
    {
        a[i] = 1;
    }

    for (int i = 0; i < n; i++)
    {
        for (int j = 0; j < n; j++)
        {

            a[i] = 1;
        }
    }
    return 0;
}
