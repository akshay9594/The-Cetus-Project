/*BHEADER****************************************************************
 * (c) 2007   The Regents of the University of California               *
 *                                                                      *
 * See the file COPYRIGHT_and_DISCLAIMER for a complete copyright       *
 * notice and disclaimer.                                               *
 *                                                                      *
 *EHEADER****************************************************************/

//--------------
//  A micro kernel 
//--------------
#include <stdio.h>
#include <stdlib.h>
//#include <omp.h>
#include <sys/time.h>
#include "headers.h"


//
const int testIter   = 500;
double totalWallTime = 0.0;
 double total_target_loop_time = 0.0;
// 
void test_Matvec();
void test_Relax();
void test_Axpy();

//
int main(int argc, char *argv[])
{
  double t0        = 0.0,
         t1        = 0.0,
         del_wtime = 0.0;

  int  max_num_threads;

  printf("\n");
  printf("//------------ \n");
  printf("// \n");
  printf("//  CORAL  AMGmk Benchmark Version 1.0 \n");
  printf("// \n");
  printf("//------------ \n");

   printf("\n testIter   = %d \n\n", testIter );  

  //t0 = omp_get_wtime();

  // Matvec
  totalWallTime = 0.0;
  total_target_loop_time = 0.0;
 
  test_Matvec();

  printf("\n");
  printf("//------------ \n");
  printf("// \n");
  printf("//   MATVEC\n");
  printf("// \n");
  printf("//------------ \n");

  printf("\nWall time = %f seconds. \n", totalWallTime);
   printf("Target loop time=%f seconds\n", total_target_loop_time);
  max_num_threads = 1;
  printf("max_num_threads =%d\n",max_num_threads);
  // // Relax
  // totalWallTime = 0.0;

  // test_Relax();

  // printf("\n");
  // printf("//------------ \n");
  // printf("// \n");
  // printf("//   Relax\n");
  // printf("// \n");
  // printf("//------------ \n");

  // printf("\nWall time = %f seconds. \n", totalWallTime);


  // // Axpy
  // totalWallTime = 0.0;
 
  // test_Axpy();

  // printf("\n");
  // printf("//------------ \n");
  // printf("// \n");
  // printf("//   Axpy\n");
  // printf("// \n");
  // printf("//------------ \n");

  // printf("\nWall time = %f seconds. \n", totalWallTime);

  // t1 = omp_get_wtime();;

  //  del_wtime = t1 - t0;

  // printf("\nTotal Wall time = %f seconds. \n", del_wtime);


  return  0;

}

void test_Matvec()
{
  // double t0 = 0.0,
  //        t1 = 0.0;
  struct timeval start, end;

  double sum = 0.0;

  hypre_CSRMatrix *A;
  hypre_Vector *x, *y, *sol;
  int nx, ny, nz, i;
  double *values;
  double *y_data, *sol_data;
  double error, diff;

  double time_array[500] = {0.0};

  nx = 60;  /* size per proc nx*ny*nz */
  ny = 60;
  nz = 60;

  values = hypre_CTAlloc(double, 4);
  values[0] = 6; 
  values[1] = -1;
  values[2] = -1;
  values[3] = -1;

  A = GenerateSeqLaplacian(nx, ny, nz, values, &y, &x, &sol);
  hypre_CSRMatrixSetRownnz(A);

  hypre_SeqVectorSetConstantValues(x,1);
  hypre_SeqVectorSetConstantValues(y,0);

gettimeofday(&start,NULL);
//t0 = omp_get_wtime();
  for (i=0; i<testIter; ++i)
      hypre_CSRMatrixMatvec(1,A,x,0,y, time_array,i);
//t1 = omp_get_wtime() ;

  gettimeofday(&end, NULL); 
	
  totalWallTime += (end.tv_sec + (double)end.tv_usec/1000000) - (start.tv_sec + (double)start.tv_usec/1000000);

  //totalWallTime += t1 - t0;

  for(i=0; i<testIter; i++){
    total_target_loop_time += time_array[i];
  }
 
  y_data = hypre_VectorData(y);
  sol_data = hypre_VectorData(sol);

  error = 0;
  for (i=0; i < nx*ny*nz*5; i++)
  {
      diff = fabs(y_data[i]-sol_data[i]);
      if (diff > error) error = diff;
  }
     
  if (error > 0) printf(" \n Matvec: error: %e\n", error);

  hypre_TFree(values);
  hypre_CSRMatrixDestroy(A);
  hypre_SeqVectorDestroy(x);
  hypre_SeqVectorDestroy(y);
  hypre_SeqVectorDestroy(sol);

}

void test_Relax()
{
 struct timeval start, end;

  hypre_CSRMatrix *A;
  hypre_Vector *x, *y, *sol;
  int nx, ny, nz, i;
  double *values;
  double *x_data;
  double diff, error;

  nx = 50;  /* size per proc nx*ny*nz */
  ny = 50;
  nz = 50;

  values = hypre_CTAlloc(double, 4);
  values[0] = 6; 
  values[1] = -1;
  values[2] = -1;
  values[3] = -1;

  A = GenerateSeqLaplacian(nx, ny, nz, values, &y, &x, &sol);

  hypre_SeqVectorSetConstantValues(x,1);

  gettimeofday(&start,NULL);
  //t0 = omp_get_wtime();
  for (i=0; i<testIter; ++i)
      hypre_BoomerAMGSeqRelax(A, sol, x);
  //t1 = omp_get_wtime();

   gettimeofday(&end, NULL); 
	
  totalWallTime += (end.tv_sec + (double)end.tv_usec/1000000) - (start.tv_sec + (double)start.tv_usec/1000000);

  x_data = hypre_VectorData(x);
  error = 0;
  for (i=0; i < nx*ny*nz; i++)
  {
      diff = fabs(x_data[i]-1);
      if (diff > error) error = diff;
  }
     
  if (error > 0) printf(" \n Relax: error: %e\n", error);

  hypre_TFree(values);
  hypre_CSRMatrixDestroy(A);
  hypre_SeqVectorDestroy(x);
  hypre_SeqVectorDestroy(y);
  hypre_SeqVectorDestroy(sol);

}

void test_Axpy()
{
  // double t0 = 0.0,
  //        t1 = 0.0;

  struct timeval start, end;

  hypre_Vector *x, *y;
  int nx, i;
  double alpha=0.5;
  double diff, error;
  double *y_data;

  nx = 125000;  /* size per proc  */

  x = hypre_SeqVectorCreate(nx);
  y = hypre_SeqVectorCreate(nx);

  hypre_SeqVectorInitialize(x);
  hypre_SeqVectorInitialize(y);

  hypre_SeqVectorSetConstantValues(x,1);
  hypre_SeqVectorSetConstantValues(y,1);

 
  gettimeofday(&start, NULL);

  //t0 = omp_get_wtime();
  for (i=0; i<testIter; ++i)
      hypre_SeqVectorAxpy(alpha,x,y);
  //t1 = omp_get_wtime();
  
  gettimeofday(&end, NULL); 
	
  totalWallTime += (end.tv_sec + (double)end.tv_usec/1000000) - (start.tv_sec + (double)start.tv_usec/1000000);

  y_data = hypre_VectorData(y);
  error = 0;
  for (i=0; i < nx; i++)
  {
    diff = fabs(y_data[i]-1-0.5*(double)testIter);
      if (diff > error) error = diff;
  }
     
  if (error > 0) printf(" \n Axpy: error: %e\n", error);

  hypre_SeqVectorDestroy(x);
  hypre_SeqVectorDestroy(y);

}

