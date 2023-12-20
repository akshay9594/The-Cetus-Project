/* 
 The SDDMM kernel
*/

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
//#include <omp.h>
#include <sys/time.h>
#include <math.h>
#include <assert.h>
#include "util.h"

#define N 30000

void sddmm_CPU_CSR(int* col_ptr, int* col_ind, double* nnz_val, double* W,
                     double* H, double* p,int n_rows ,int k, int nonzeros);

// void Par_sddmm_CPU_CSR(int* par_row_ptr, int* col_ind, double* nnz_val, double* W,
//                      double* H, double* p,int n_rows ,int k, int nonzeros);

int* row_val; 
int* col_val; 
double* nnz_val;

int* col;
//int* rowP;

int convertStrtoArr(char* str)
{
    // get length of string str
    int str_length = strlen(str);
  
    // create an array with size as string
    // length and initialize with 0
    int arr =0;
  
    int j = 0, i, sum = 0;
  
    // Traverse the string
    for (i = 0; str[i] != '\0'; i++) {
  
        // if str[i] is ', ' then split
        if (str[i] == ',')
            continue;
         if (str[i] == ' '){
            // Increment j to point to next
            // array location
            break;
        }
        else {
  
            // subtract str[i] by 48 to convert it to int
            // Generate number by multiplying 10 and adding
            // (int)(str[i])
   
          if(str[i] - 48 >= 0)
            arr = arr * 10 + (str[i] - 48);
              
        }
    }
   
      return arr;
   
  
}

void sddmm_CPU_CSR(int* col_ptr, int* row_ind, double* nnz_val, double* W,
                     double* H, double* p,int n_cols ,int k, int nonzeros){
       // reduction(+:rmse)
    int i,r, ind,t,holder;
    double sm;

      holder=1;
     col_ptr[0]=0;
     r = col_val[0];
      for(i =0; i < nonzeros; i++){
        if(col_val[i] != r){
            col_ptr[holder++] = i;
           // rowP[holder] = i;
            r = col_val[i];
        }
    }

    col_ptr[holder] = nonzeros;

    //#pragma omp parallel for private(sm,r,ind,t)
    for (r = 0; r < n_cols; ++r){
        for (ind = col_ptr[r]; ind < col_ptr[r+1]; ++ind){
            sm=0;
            for (t = 0; t < k; ++t){
                sm += W[r * k + t] * H[row_ind[ind] * k + t];
               
            }
            p[ind] = sm * nnz_val[ind];     //Scaling of non-zero elements of the sparse matrix
           
        }                
    } 
}

// void Par_sddmm_CPU_CSR(int* par_row_ptr, int* col_ind, double* nnz_val, double* W,
//                      double* H, double* p,int n_rows ,int k, int nonzeros){
//        // reduction(+:rmse)
//     int i,r, ind,t,holder;
//     double sm;

//      holder =0;
//     par_row_ptr[0]=0;
//      r = row_val[0];
//       for(i =0; i < nonzeros; i++){
//         if(row_val[i] != r){
//             par_row_ptr[holder++] = i;
//            // rowP[holder] = i;
//             r = row_val[i];
//         }
//     }
    
//     par_row_ptr[holder] = nonzeros;

//     #pragma omp parallel for private(sm,r,ind,t)
//     for (r = 0; r < n_rows; ++r){
//         for (ind = par_row_ptr[r]; ind < par_row_ptr[r+1]; ++ind){
//             sm=0;
//             for (t = 0; t < k; ++t){
//                 sm += W[r * k + t] * H[col_ind[ind] * k + t];
               
//             }
//             p[ind] = sm * nnz_val[ind];     //Scaling of non-zero elements of the sparse matrix
           
//         }                
//     } 
// }

int main(int argc, char *argv[]){

     if(argc < 2){
   printf("Input missing arguments, you need to specify input list file\n");
  }

 char* file_path = argv[1];
  //std::string inputMatrix;
  int i,j,k, s_factor,holder,count;
  int num_rows, num_cols, nonzeros,num_runs,failed;
  size_t len,read;
  struct timeval start,end;//,startTT, endTT;
  FILE * fp;
  char *line = NULL;
  char *ptr;
  char* rowstr = NULL;
  char* temp_str;
  double seconds, total_time, init_time;
  double* W;
  double* H;
  double* P;
  double* ParallelP;

    s_factor = 100;
    count = 0;
    num_runs = 5;

    fp = fopen(file_path, "r");
    if (fp == NULL)
        exit(EXIT_FAILURE);

    while ((read = getline(&line, &len, fp)) != -1) {
        if(line[0] == '%'){
            continue;
        }
         else{
            break;
         }
    }

   char delim[] = " ";

   ptr = strtok(line, delim);
   rowstr = (char *)malloc(sizeof(line)); 

   while(ptr != NULL)
	{
      if(count ==0){  
         memcpy(rowstr, ptr, sizeof(ptr));
         num_rows = convertStrtoArr(ptr);
      }
      else if(count == 1){
        num_cols = convertStrtoArr(ptr);
      }
      else if(count == 2){
         nonzeros = convertStrtoArr(ptr);
      }
      count++;
		ptr = strtok(NULL, delim);
	}

   row_val = (int*)malloc(sizeof(int)*(nonzeros+1));
   col_val = (int*)malloc(sizeof(int)*(nonzeros+1));
   nnz_val = (double*)malloc(sizeof(double)*(nonzeros+1));


   i=0;
   while ((read = getline(&line, &len, fp)) != -1) {
       char *ptr = strtok(line, delim);
       count = 0;
       while(ptr != NULL){
         if(count ==0){  
            row_val[i]= convertStrtoArr(ptr);
         }
         else if(count == 1){
            col_val[i] = convertStrtoArr(ptr);
         }
         else if(count == 2){
            nnz_val[i] =strtod(ptr, &temp_str);
         }
          count++; 
          ptr = strtok(NULL, delim);
         
      }
       i++; 
   }

   col = malloc(sizeof(int)*(nonzeros+1));
   //rowP = malloc(sizeof(int)*(nonzeros+1));

  
    total_time=0.0;

    W =  (double*)malloc(sizeof(double)*(num_rows*s_factor+s_factor));
    H =  (double*)malloc(sizeof(double)*(num_cols*s_factor+s_factor));
    P =  (double*)malloc(sizeof(double)*(nonzeros+1));
    //ParallelP =  (double*)malloc(sizeof(double)*(nonzeros+1));
    initialize(W,num_rows,s_factor);
    initialize(H,num_cols,s_factor);

   for(k=0; k < num_runs; k++){

      gettimeofday(&start,NULL);

      sddmm_CPU_CSR(col,row_val,nnz_val,W,H,P,num_cols,s_factor,nonzeros);

       gettimeofday(&end, NULL);

      seconds = (end.tv_sec + (double)end.tv_usec/1000000) - (start.tv_sec + (double)start.tv_usec/1000000); 

      total_time+= seconds;

     // gettimeofday(&start,NULL);

    //   //  Parallel Run
    //   Par_sddmm_CPU_CSR(rowP,col_val,nnz_val,W,H,ParallelP,num_rows,s_factor,nonzeros);

    //   gettimeofday(&end, NULL);

    //   seconds = (end.tv_sec + (double)end.tv_usec/1000000) - (start.tv_sec + (double)start.tv_usec/1000000); 

    //   total_timeP+= seconds;

   }

    // failed = 0;
    // for (i = 0; i < num_rows; ++i) {
    //     printf("P[%d]=%f,\n",i ,P[i]);
    //   //if(P[i]-ParallelP[i] > 10e-4) failed=1;
    // }

    // if(failed == 1){
    //   printf("Verification failed!!!");
    //   exit(0);
    // }

   printf("Input File Read successfully\n");
   
   printf("-->Avg time taken by the serial kernel for %d runs = %f s\n", num_runs,total_time/num_runs);
   //printf("-->Avg time taken by the Parallel kernel for %d runs = %f s\n", num_runs,total_timeP/num_runs);


    fclose(fp);
    if (line)
      free(line);
    exit(EXIT_SUCCESS);

   return 0;
}

