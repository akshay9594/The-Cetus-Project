/* Inner product
*/
 
int dot_product(int *a, int *b, int n) {
    int i,sum = 0;
 
    for (i = 0; i < n; i++) {
    	sum += a[i] * b[i];
    }
 
    return sum;
}

int main () {

    int a[10000], b[10000];
 
 	int i;
 	for (i = 0; i < 10000; i++) {	
 		a[i] = -1;
 		b[i] = 1;
 	}
    
    dot_product(a, b, sizeof(a) / sizeof(a[0]));

	return 0;
}
