/*  Matrix multiplication
*/

int main(){

  int first[1000][1000], second[1000][1000], multiply[1000][1000];
  int c, d, k, sum = 0;
  int m,p,q;
  m = p = q = 1000;
  
    for (c = 0; c < m; c++) {
      for (d = 0; d < q; d++) {
        for (k = 0; k < p; k++) {
          sum = sum + first[c][k]*second[k][d];
        }
        multiply[c][d] = sum;
        sum = 0;
      }
    }
	
   return 0;
}
