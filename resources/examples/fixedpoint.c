#include <stdio.h>
#include <stdlib.h>

// gcc -o fixed fixedpoint.c
//./fixed

typedef struct
{
    int intSize,decSize,integer,decimal,number;
    unsigned int data;
}fixed;


fixed create(int isize,int dsize, float data)
{
    union
    {
        float dat;
        int dato;
    } a = {.dat = data};

    fixed aux;
    aux.intSize = isize-1;
    aux.decSize = dsize-1;
    aux.integer = (unsigned int)data;
    aux.decimal =(((a.dato))<<9)<<(((a.dato)>>23)-127) ;
    aux.decimal = (unsigned)aux.decimal >> (32-dsize);
    //aux.data = aux.integer<<31-fixed_size;
    //aux.data = aux.integer<<31-fixed_size | (unsigned)(aux.decimal<<31-fixed_size)>>fixed_size+1;
    return aux;
}

void bin(unsigned n, int nsize)
{
    for (unsigned i = 1 << nsize; i > 0; i = i / 2)
        (n & i)? printf("1"): printf("0");
}

float toFloat(fixed n)
{
    float f = 0,a;
    for (unsigned i = 1 << (n.intSize-1); i > 0; i = i / 2)
        (n.integer & i)? (f = f + i ): f;
    for (unsigned i = 1 << n.decSize; i > 0; i = i / 2)
    {
        a = (float)(1)/((1 << n.decSize)/i);
        (n.decimal & i)? (f = f + a/2): f;
    }
    return f;
}


void print(fixed n)
{
    bin(n.integer,n.intSize);
    //printf(".");
    bin(n.decimal,n.decSize);
}

int main()
{
    fixed num1 = create(4, 28, (3.2453));
    printf("\nNumber : ");
    printf("%f \n",toFloat(num1));
    printf("Fixed point number\n");
    print(num1);
    printf("\n");
    //bin(num1.data,2*num1.fixed_size);
    //printf("\n");
    return 0;
}
