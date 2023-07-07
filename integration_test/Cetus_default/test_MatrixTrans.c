/* Transpose matrix
*/

int main () {

 int A[1005][1005], T[1005][1005];

 int i,j;
 for (i = 0; i<1005; i++) {
 	for (j = 0; j<1005; j++) {
 		A[i][j] = i+j;
 	}
 }

 for (i = 0; i<1005; i++) {
 	for (j = 0; j<1005; j++) {
 		T[i][j] = A[j][i];
 	}
 }

 return 0;
}
