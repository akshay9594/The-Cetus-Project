/*  
    Subscripted Subscript example from Gromacs - SPEC CPU 2006
*/

#include <stdio.h>
#include <math.h>
#include <stdlib.h>

#define N 30000
#define M 10000

int main(){

   int bla1[N], bla2[N], Z[N], blm[N], blcc[N], rhs1[N], iatom[3*N+2];
    int sol[N], bllen[N], blnr[N], r[N][3], q[N], xp[N][3], X[N], Y[N][N];

    int i,j,k,n,b,tmp0,tmp1,tmp2,mvb;
    int a1,a2, start;

    blnr[0] = 0;
    for(i=0; i<N; i++){
        a1 = iatom[3*i+1];
        a2 = iatom[3*i+2];
        blnr[i+1] = blnr[i];
        for(k=0; k<M; k++){
            if(Y[a1-start][k] != i)
                Z[blnr[i+1]++] = Y[a1-start][k];
        }
        for(k=0; k<M; k++){
            if(Y[a2-start][k] != i)
                Z[blnr[i+1]++] = Y[a2-start][k];
            }
    }

   //loop to parallelize
    for(b=0; b<N; b++){
        tmp0 = r[b][0];
        tmp1 = r[b][1];
        tmp2 = r[b][2];
        i = bla1[b];
        j = bla2[b];
        for(n=blnr[b]; n<blnr[b+1]; n++){
            k = Z[n];
            blm[n]= blcc[n]*(tmp0*r[k][0]+tmp1*r[k][1]+tmp2*r[k][2]);
        } 
        mvb=q[b]*(tmp0*(xp[i][0]-xp[j][0])+
                tmp1*(xp[i][1]-xp[j][1])+
                tmp2*(xp[i][2]-xp[j][2])-bllen[b]);
        rhs1[b]=mvb;
        sol[b]=mvb;
    }

    return 0;


}
