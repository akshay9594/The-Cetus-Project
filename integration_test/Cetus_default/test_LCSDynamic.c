/*  LCS dynamic programming
*/
#define MAX(a,b) ((a) > (b) ? a : b)

int main(){
 
 	int M[10000][10000];
 	char X[10000];
 	char Y[10000];
 
    for (int i = 0; i < 10000; i++) 
        for (int j = 0; j < 10000; j++) 
            if (i==0 || j==0)
                M[i][j] = 0;
            else
            	if (X[i]==Y[j])
                	M[i][j] = M[i-1][j-1] + 1;
 				else
 					M[i][j] = MAX(M[i-1][j], M[i][j-1]);
 				
    return 0;

}
