#include <stdio.h>
#include <omp.h>

int main()
{

    int threadn;
    #pragma omp parallel
    {
        threadn = omp_get_thread_num();
        printf("The parallel region is executed by thread%d\n", threadn);
    }

    return 0;
}
