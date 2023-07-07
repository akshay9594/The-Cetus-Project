/* Shear sort algorithm
*/ 

//#include <math.h>
//#include <stdio.h>

int n;
int a[1005][1005];

/*
void rowsort(int i){
    for(int j=0;j<n-1;j++){
        for(int k=0;k<n-j-1;k++){
            if(a[i][k]>a[i][k+1]){
                int temp=a[i][k];
                a[i][k]=a[i][k+1];
                a[i][k+1]=temp;
            }
        }
    }
}

void rowrevsort(int i){
    for(int j=0;j<n-1;j++){
        for(int k=0;k<n-j-1;k++){
            if(a[i][k]<a[i][k+1]){
                int temp=a[i][k];
                a[i][k]=a[i][k+1];
                a[i][k+1]=temp;
            }
        }
    }
}

void colsort(int i){
    for(int j=0;j<n-1;j++){
        for(int k=0;k<n-j-1;k++){
            if(a[k][i]>a[k+1][i]){
                int temp=a[k][i];
                a[k][i]=a[k+1][i];
                a[k+1][i]=temp;
            }
        }
    }
}
*/

int main () {

    n = 1005;
    
    for(int j=0;j<n;j++){
        for(int k=0;k<n;k++){
            a[j][k]=n-j;
        }
    }
    
    int m=(int)ceil(log2(n));
    
    for(int i=0;i<m;i++){
        for(int j=0;j<n;j++){
            if(j%2==0){
            	//rowsort(j);
            	for(int t=0;t<n-1;t++){
        			for(int k=0;k<n-t-1;k++){
            			if(a[j][k]>a[j][k+1]){
                			int temp=a[j][k];
                			a[j][k]=a[j][k+1];
                			a[j][k+1]=temp;
            			}
        			}
    			}
            }else{
            	//rowrevsort(j);
            	for(int t=0;t<n-1;t++){
       	 			for(int k=0;k<n-t-1;k++){
            			if(a[j][k]<a[j][k+1]){
                			int temp=a[j][k];
                			a[j][k]=a[j][k+1];
                			a[j][k+1]=temp;
            			}
        			}
    			}
            }
        }
        for(int j=0;j<n;j++) { 
        	//colsort(j);
        	for(int t=0;t<n-1;t++){
        		for(int k=0;k<n-t-1;k++){
            		if(a[k][j]>a[k+1][j]){
                		int temp=a[k][j];
                		a[k][j]=a[k+1][j];
                		a[k+1][j]=temp;
            		}
        		}
    		}
        }
    }	
        	
    for(int j=0;j<n;j++){
        if(j%2==0){
        	//rowsort(j);
        	for(int t=0;t<n-1;t++){
        		for(int k=0;k<n-t-1;k++){
            		if(a[j][k]>a[j][k+1]){
                		int temp=a[j][k];
                		a[j][k]=a[j][k+1];
                		a[j][k+1]=temp;
            		}
        		}
    		}
        }else{
            //rowrevsort(j);
            for(int t=0;t<n-1;t++){
       	 		for(int k=0;k<n-t-1;k++){
            		if(a[j][k]<a[j][k+1]){
                		int temp=a[j][k];
                		a[j][k]=a[j][k+1];
                		a[j][k+1]=temp;
            		}
        		}
    		}
        }
    }
    /*
	printf("\n Matrix of input data:\n");
	int i,j;
	for(i=0;i<n;i++)  {
  		for(j=0;j<n;j++) 
   			printf("%d \t",a[i][j]);
  		printf("\n");
 	}
    */

 	return 0;

}
