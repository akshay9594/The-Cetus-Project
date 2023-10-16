#include <stdio.h>
#include <stdlib.h>

static unsigned len = (8 << 20);

void func(int *a)
{
    int i, j;

    for (i = 0; i < len; i += 4)
    {
        for (j = 0; j < 4; j++)
        {
            a[i + j]++;
        }
    }
}

int main()
{
    int *a;
    a = (int *)malloc(len * sizeof(int));

    func(a);

    free(a);
}
