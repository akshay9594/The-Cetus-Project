/* Diagonally dominant matrix
*/

int main () {

	int A[10005][10005];

	int i,j;
 	for (i = 0; i<10005; i++) {
 		for (j = 0; j<10005; j++) {
 			A[i][j] = i+j;
 		}
 	}

 	int boolean = 1;
 	int sum = 0;
 	for (i = 0; i<10005; i++) {
 		for (j = 0; j<10005; j++) {
 			if (i!=j) {
 				sum += A[i][j];
 			}
 		}
 		if (A[i][i]<sum) {
 			boolean = 0;
 			break;
 		}
 		sum = 0;
 	}


	return 0;

}
