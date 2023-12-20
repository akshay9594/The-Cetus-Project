#include <stdio.h>
#include <stdlib.h>
#include <time.h>


void initialize(double *X, int n, int k){
    srand48(0L);
    for(int r = 0; r < n; ++r){
        for(int t = 0; t < k; ++t)
            X[r*k+t] = 0.1*drand48(); //-1;
    }
}