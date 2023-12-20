/* Floyd Warshall algorithm
*/

//#include<stdio.h>

/*
int min(int a, int b) {
	if(a<b) {
  		return a;
 	} else {
  		return b;
  	}
}

void floyds(int p[1005][1005],int n) {
	int i,j,k;
 	for(k=1;k<=n;k++) {
  		for(i=1;i<=n;i++) { 
   			for(j=1;j<=n;j++) {
    			if(i==j) {
     				p[i][j]=0;
    			} else {
     				p[i][j]=min(p[i][j],p[i][k]+p[k][j]);
				}
			}
		}
	}
}
*/


int main () {

 	int p[1005][1005],w,e,u,v;
 	int n = 1004;
 	/*
 	printf("\n Enter the number of vertices:");
 	scanf("%d",&n);
 	printf("\n Enter the number of edges:\n");
 	scanf("%d",&e);
 	*/
 	
 	int i,j;
	for(i=1; i<=n; i++) {
  		for(j=1;j <=n; j++) {
   			p[i][j]=999;
 		}
 	}
 	
 	p[10][20]=22;
 	p[20][30]=55;
 	p[30][40]=33;
 	p[40][50]=44;
 	p[50][60]=66;
 	p[60][70]=88;
 	p[70][10]=77;
 	
 	/*	
 	for(i=1;i<=e;i++) {
  		printf("\n Enter the end vertices of edge%d with its weight \n",i);
  		scanf("%d%d%d",&u,&v,&w);
  		p[u][v]=w;
 	}
 	*/
 
 	/*
	printf("\n Matrix of input data:\n");
	for(i=1;i<=n;i++)  {
  		for(j=1;j<=n;j++) 
   			printf("%d \t",p[i][j]);
  		printf("\n");
 	}
 	*/
 	
 	//floyds(p,n);
 	
 	int k;
 	for(k=1;k<=n;k++) {
  		for(i=1;i<=n;i++) { 
   			for(j=1;j<=n;j++) {
    			if(i==j) {
     				p[i][j]=0;
    			} else {
    				if (p[i][j]<(p[i][k]+p[k][j]))
    					p[i][j] = p[i][j];
    				else
     					p[i][j]=p[i][k]+p[k][j];
				}
			}
		}
	}
 
  	/*
 	printf("\n Transitive closure:\n");
 	for(i=1;i<=n;i++) {
  		for(j=1;j<=n;j++)
   			printf("%d \t",p[i][j]);
  		printf("\n");
 	}
 
 	printf("\n The shortest paths are:\n");
 	for(i=1;i<=n;i++)
  		for(j=1;j<=n;j++) {
   			if(i!=j)
    		printf("\n <%d,%d>=%d",i,j,p[i][j]);
  	}
  	*/
 
	return 0;

}
