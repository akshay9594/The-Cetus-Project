/* bitonic sort
*/

//#include <stdio.h>


        int main() {
          int n, *arr, i,s;
          int arr1[16] = {3,5,8,9,10,12,14,20,95,90,60,40,35,23,18,0};
          arr = arr1; 
          n = 16;
          // print array before 
          //printArray(arr,n);
		
          // do merges
          for (s=2; s <= n; s*=2) {
            for (i=0; i < n; i += s*2) {
              
              	//merge_down((arr+i+s),s);
            	int step=n/2,i,j,k,temp;
          		while (step > 0) {
            		for (i=0; i < n; i+=step*2) {
              			for (j=i,k=0; k < step; j++,k++) {
            				if (arr[j] < arr[j+step]) {
              				// swap
              					temp = arr[j];
              					arr[j]=arr[j+step];
              					arr[j+step]=temp;
            				}
              			}
            		}
            	step /= 2;
          		}
              
              	//merge_up((arr+i),s); 
              	step=n/2;
          		while (step > 0) {
            		for (i=0; i < n; i+=step*2) {
              			for (j=i,k=0; k < step; j++,k++) {
            				if (arr[j] > arr[j+step]) {
              				// swap
              					temp = arr[j];
              					arr[j]=arr[j+step];
            				  	arr[j+step]=temp;
            				}
              			}
            		}
            	step /= 2;
          		}
        	
            }
          }

          //printArray(arr,n);
          
          return 0;
        }
