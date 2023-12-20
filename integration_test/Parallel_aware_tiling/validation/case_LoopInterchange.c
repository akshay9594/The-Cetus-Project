/*
Copyright (C) 1991-2022 Free Software Foundation, Inc.
   This file is part of the GNU C Library.

   The GNU C Library is free software; you can redistribute it andor
   modify it under the terms of the GNU Lesser General Public
   License as published by the Free Software Foundation; either
   version 2.1 of the License, or (at your option) any later version.

   The GNU C Library is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
   Lesser General Public License for more details.

   You should have received a copy of the GNU Lesser General Public
   License along with the GNU C Library; if not, see
   <https:www.gnu.org/licenses/>. 
*/
/*
This header is separate from features.h so that the compiler can
   include it implicitly at the start of every compilation.  It must
   not itself include <features.h> or any other header that includes
   <features.h> because the implicit include comes before any feature
   test macros that may be defined in a source file before it first
   explicitly includes a system header.  GCC knows the name of this
   header in order to preinclude it. 
*/
/*
glibc's intent is to support the IEC 559 math functionality, real
   and complex.  If the GCC (4.9 and later) predefined macros
   specifying compiler intent are available, use them to determine
   whether the overall intent is to support these features; otherwise,
   presume an older compiler has intent to support these features and
   define these macros by default. 
*/
/*
wchar_t uses Unicode 10.0.0.  Version 10.0 of the Unicode Standard is
   synchronized with ISOIEC 10646:2017, fifth edition, plus
   the following additions from Amendment 1 to the fifth edition:
   - 56 emoji characters
   - 285 hentaigana
   - 3 additional Zanabazar Square characters
*/
/*




Different loops to test the Loop Interchange pass in Cetus.




*/
int a[10000][10000], c[10000], b[10000][10000], d[10000][10000];
int work[10000][10000][10000], coef2[1000][10000], coef4[10000][10000];
int S[10000], x[10000][10000], y[10000][10000], f[10000][10000], e[10000][10000];
int main()
{
	int i, j, k, n = 10000, r = 1000, jmi, ld1, ld2, ldi, ld, m;
	int _ret_val_0;
	int cores = 4;
	int cacheSize = 8192;
	if ((100000000<=100000)&&(cacheSize>159984))
	{
		#pragma loop name main#0 
		#pragma cetus private(i, j) 
		#pragma cetus parallel 
		#pragma omp parallel for private(i, j)
		for (i=0; i<10000; i ++ )
		{
			#pragma loop name main#0#0 
			#pragma cetus private(j) 
			for (j=0; j<10000; j ++ )
			{
				b[j][i]=(2*b[j+1][i-1]);
			}
		}
	}
	else
	{
		int balancedTileSize = ((cacheSize/4)/cores);
		int ii;
		int iTile = balancedTileSize;
		#pragma loop name main#1 
		#pragma cetus private(i, ii, j) 
		#pragma cetus parallel 
		#pragma omp parallel for private(i, ii, j)
		for ((ii=0); ii<10000; ii+=iTile)
		{
			#pragma loop name main#1#0 
			#pragma cetus private(i, j) 
			for ((i=ii); i<(((iTile+ii)<10000) ? (iTile+ii) : 10000); i ++ )
			{
				#pragma loop name main#1#0#0 
				#pragma cetus private(j) 
				for (j=0; j<10000; j ++ )
				{
					b[j][i]=(2*b[j+1][i-1]);
				}
			}
		}
	}
	/* Taken from ARC2D (Perfect Benchmarks) */
	if ((100000000<=100000)&&(cacheSize>159984))
	{
		#pragma loop name main#2 
		#pragma cetus private(j, k) 
		#pragma cetus parallel 
		#pragma omp parallel for private(j, k)
		for (k=0; k<10000; k ++ )
		{
			#pragma loop name main#2#0 
			#pragma cetus private(j) 
			for (j=0; j<10000; j ++ )
			{
				work[j][k][3]=((coef2[j][k]*work[j][k][1])-(coef4[j][k]*work[j][k][2]));
			}
		}
	}
	else
	{
		int balancedTileSize = ((cacheSize/4)/cores);
		int kk;
		int kTile = balancedTileSize;
		#pragma loop name main#3 
		#pragma cetus private(j, k, kk) 
		#pragma cetus parallel 
		#pragma omp parallel for private(j, k, kk)
		for ((kk=0); kk<10000; kk+=kTile)
		{
			#pragma loop name main#3#0 
			#pragma cetus private(j, k) 
			for ((k=kk); k<(((kTile+kk)<10000) ? (kTile+kk) : 10000); k ++ )
			{
				#pragma loop name main#3#0#0 
				#pragma cetus private(j) 
				for (j=0; j<10000; j ++ )
				{
					work[j][k][3]=((coef2[j][k]*work[j][k][1])-(coef4[j][k]*work[j][k][2]));
				}
			}
		}
	}
	/* From ARC2D Perfect benchmarks */
	if (((n*n)<=100000)&&(cacheSize>159984))
	{
		#pragma loop name main#4 
		#pragma cetus private(j, k, ld, ld1, ld2, ldi) 
		#pragma cetus parallel 
		#pragma omp parallel for private(j, k, ld, ld1, ld2, ldi)
		for (k=0; k<n; k ++ )
		{
			#pragma loop name main#4#0 
			#pragma cetus private(j, ld, ld1, ld2, ldi) 
			for (j=0; j<n; j ++ )
			{
				ld2=a[j][k];
				ld1=(b[j][k]-(ld2*x[j-2][k]));
				ld=(d[j][k]-((ld2*y[j-2][k])+(ld1*x[j-2][k])));
				ldi=(1.0/ld);
				f[j][k]=(((f[j][k]-(ld2*f[j-2][k]))-(ld1*f[j-1][k]))*ldi);
				x[j][k]=((d[j][k]-(ld1*y[j-1][k]))*ld1);
				y[j][k]=(e[j][k]*ldi);
			}
		}
	}
	else
	{
		int balancedTileSize = ((cacheSize/4)/cores);
		int jj;
		int jTile = balancedTileSize;
		#pragma loop name main#5 
		#pragma cetus private(j, jj, k, ld, ld1, ld2, ldi) 
		#pragma cetus parallel 
		#pragma omp parallel for private(j, jj, k, ld, ld1, ld2, ldi)
		for ((jj=0); jj<n; jj+=jTile)
		{
			#pragma loop name main#5#0 
			#pragma cetus private(j, k, ld, ld1, ld2, ldi) 
			for (k=0; k<n; k ++ )
			{
				#pragma loop name main#5#0#0 
				#pragma cetus private(j, ld, ld1, ld2, ldi) 
				for ((j=jj); j<(((jTile+jj)<n) ? (jTile+jj) : n); j ++ )
				{
					ld2=a[j][k];
					ld1=(b[j][k]-(ld2*x[j-2][k]));
					ld=(d[j][k]-((ld2*y[j-2][k])+(ld1*x[j-2][k])));
					ldi=(1.0/ld);
					f[j][k]=(((f[j][k]-(ld2*f[j-2][k]))-(ld1*f[j-1][k]))*ldi);
					x[j][k]=((d[j][k]-(ld1*y[j-1][k]))*ld1);
					y[j][k]=(e[j][k]*ldi);
				}
			}
		}
	}
	/* Matrix Multiplication kernel */
	if ((((m*n)*n)<=100000)&&(cacheSize>(-16+(16*n))))
	{
		#pragma loop name main#6 
		#pragma cetus private(i, j, k) 
		#pragma cetus parallel 
		#pragma omp parallel for private(i, j, k)
		for (i=0; i<n; i ++ )
		{
			#pragma loop name main#6#0 
			#pragma cetus private(j, k) 
			for (j=0; j<m; j ++ )
			{
				#pragma loop name main#6#0#0 
				#pragma cetus private(k) 
				/* #pragma cetus reduction(+: d[i][j])  */
				for (k=0; k<n; k ++ )
				{
					d[i][j]=(d[i][j]+(a[i][k]*b[k][j]));
				}
			}
		}
	}
	else
	{
		int balancedTileSize = ((cacheSize/4)/cores);
		int kk;
		int kTile = balancedTileSize;
		#pragma loop name main#7 
		#pragma cetus private(i, j, k, kk) 
		#pragma cetus parallel 
		#pragma omp parallel for private(i, j, k, kk)
		for (i=0; i<n; i ++ )
		{
			#pragma loop name main#7#0 
			#pragma cetus private(j, k, kk) 
			/* #pragma cetus reduction(+: d[i][j])  */
			for ((kk=0); kk<n; kk+=kTile)
			{
				#pragma loop name main#7#0#0 
				#pragma cetus private(j, k) 
				for (j=0; j<m; j ++ )
				{
					#pragma loop name main#7#0#0#0 
					#pragma cetus private(k) 
					/* #pragma cetus reduction(+: d[i][j])  */
					for ((k=kk); k<(((kTile+kk)<n) ? (kTile+kk) : n); k ++ )
					{
						d[i][j]=(d[i][j]+(a[i][k]*b[k][j]));
					}
				}
			}
		}
	}
	if (((n*n)<=100000)&&(cacheSize>(-16+(16*n))))
	{
		#pragma loop name main#8 
		#pragma cetus private(i, j) 
		#pragma cetus parallel 
		#pragma omp parallel for private(i, j)
		for (i=0; i<n; i ++ )
		{
			#pragma loop name main#8#0 
			#pragma cetus private(j) 
			for (j=0; j<n; j ++ )
			{
				a[j][i]=(0.2*((((b[j][i]+b[j-1][i])+b[j][i-1])+b[j+1][i])+b[j][i+1]));
			}
		}
	}
	else
	{
		int balancedTileSize = ((cacheSize/4)/cores);
		int jj;
		int jTile = balancedTileSize;
		#pragma loop name main#9 
		#pragma cetus private(i, j, jj) 
		#pragma cetus parallel 
		#pragma omp parallel for private(i, j, jj)
		for ((jj=0); jj<n; jj+=jTile)
		{
			#pragma loop name main#9#0 
			#pragma cetus private(i, j) 
			for (i=0; i<n; i ++ )
			{
				#pragma loop name main#9#0#0 
				#pragma cetus private(j) 
				for ((j=jj); j<(((jTile+jj)<n) ? (jTile+jj) : n); j ++ )
				{
					a[j][i]=(0.2*((((b[j][i]+b[j-1][i])+b[j][i-1])+b[j+1][i])+b[j][i+1]));
				}
			}
		}
	}
	_ret_val_0=0;
	return _ret_val_0;
}
