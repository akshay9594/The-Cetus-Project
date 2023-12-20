/* Examples
*/

int main () {

int a[10], b[10], c[10], d[100000000][100000000];

int k;
for (k=0; k<10; k++) {
	a[k] = k;
	b[k] = k-10;
	c[k] = 1;
}

//Flow dependence
int i;
for (i=1; i<10000; i++) {
	a[i] = b[i];
	c[i] = a[i-1];
}

for (i=1; i<10000; i++) {
	a[i] = b[i];
	c[i] = a[i] + b[i-1];
}

//Antidependence
for (i=1; i<10000; i++) {
	a[i-1] = b[i];
	c[i] = a[i];
}

//Output dependence
for (i=1; i<10000; i++) {
	a[i] = b[i];
	a[i+1] = c[i];
}


int j;
for (i=0; i<10000; i++) {
	for (j=0; j<10000; j++) {
		d[i][j] = i+j;	
	}
}

//loop interchange
for (i=0; i<10000; i++) {
	for (j=0; j<10000; j++) {
		d[i+1][j+2] = d[i][j]+1;	
	}
}

for (i=0; i<10000; i++) {
	for (j=0; j<10000; j++) {
		d[i][j+2] = d[i][j]+1;	
	}
}

for (i=0; i<10000; i++) {
	for (j=0; j<10000; j++) {
		d[i+1][j-2] = d[i][j]+1;	
	}
}



return 0;

}
