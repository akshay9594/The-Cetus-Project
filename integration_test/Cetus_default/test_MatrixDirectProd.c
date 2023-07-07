/*  Kronecker product
*/

//#include <stdio.h>

int main(){
 
 	int A[10][10];
    int B[10][10];
 	int C[1000][1000]; 
 	
 	for (int c = 0; c<10; c++) {
 		for (int d = 0; d<10; d++) {
 			A[c][d] = 1;
 			B[c][d] = 2;
 		}
 	}
 	
 	int i, k, j, l;
 	
    for (i = 0; i < 10; i++) {
        for (k = 0; k < 10; k++) {
            for (j = 0; j < 10; j++) { 
                for (l = 0; l < 10; l++) {
                 //printf ("row: %d",i+l+1);
                 //printf (" col: %d \n",j+k+l);
                    C[i + l + 1][j + k + 1] = A[i][j] * B[k][l];
 				}
 			}
 		}
 	}
 	
    return 0;

}
