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
/* Compute Jacobian; Take about 5 seconds to finish on single core */
float a[(1024+2)][(1024+2)];
float b[(1024+2)][(1024+2)];
int main()
{
	int i, j, k;
	/* printf("JACOBI OMP VERSION: MATRIX = %d x %d, ITERATION = %d\n", SIZE, SIZE, ITER); */
	int _ret_val_0;
	int cores = 4;
	int cacheSize = 8192;
	if ((1052676<=100000)&&(cacheSize>16400))
	{
		#pragma loop name main#0 
		#pragma cetus private(i, j) 
		#pragma cetus parallel 
		#pragma omp parallel for private(i, j)
		for (i=0; i<(1024+2); i ++ )
		{
			#pragma loop name main#0#0 
			#pragma cetus private(j) 
			for (j=0; j<(1024+2); j ++ )
			{
				a[i][j]=0;
				b[i][j]=0;
			}
		}
	}
	else
	{
		int balancedTileSize = ((cacheSize/4)/cores);
		int jj;
		int jTile = balancedTileSize;
		#pragma loop name main#1 
		#pragma cetus private(i, j, jj) 
		#pragma cetus parallel 
		#pragma omp parallel for private(i, j, jj)
		for ((jj=0); jj<(1024+2); jj+=jTile)
		{
			#pragma loop name main#1#0 
			#pragma cetus private(i, j) 
			for (i=0; i<(1024+2); i ++ )
			{
				#pragma loop name main#1#0#0 
				#pragma cetus private(j) 
				for ((j=jj); j<(((jTile+jj)<(1024+2)) ? (jTile+jj) : (1024+2)); j ++ )
				{
					a[i][j]=0;
					b[i][j]=0;
				}
			}
		}
	}
	/* left and right boundary initialization */
	if (((1024+2)<=100000)&&(cacheSize>16400))
	{
		#pragma loop name main#2 
		#pragma cetus private(j) 
		#pragma cetus parallel 
		#pragma omp parallel for private(j)
		for (j=0; j<(1024+2); j ++ )
		{
			b[j][0]=1.0;
			b[j][1024+1]=1.0;
		}
	}
	else
	{
		int balancedTileSize = ((cacheSize/4)/cores);
		#pragma loop name main#3 
		#pragma cetus private(j) 
		#pragma cetus parallel 
		#pragma omp parallel for private(j)
		for (j=0; j<(1024+2); j ++ )
		{
			b[j][0]=1.0;
			b[j][1024+1]=1.0;
		}
	}
	/* upper and lower boundary initialization */
	if (((1024+2)<=100000)&&(cacheSize>16400))
	{
		#pragma loop name main#4 
		#pragma cetus private(i) 
		#pragma cetus parallel 
		#pragma omp parallel for private(i)
		for (i=0; i<(1024+2); i ++ )
		{
			b[0][i]=1.0;
			b[1024+1][i]=1.0;
		}
	}
	else
	{
		int balancedTileSize = ((cacheSize/4)/cores);
		#pragma loop name main#5 
		#pragma cetus private(i) 
		#pragma cetus parallel 
		#pragma omp parallel for private(i)
		for (i=0; i<(1024+2); i ++ )
		{
			b[0][i]=1.0;
			b[1024+1][i]=1.0;
		}
	}
	/*
	
	
	-- Timing starts before the main loop --
	
	  
	*/
	if ((2147483647<=100000)&&(cacheSize>3184))
	{
		#pragma loop name main#6 
		#pragma cetus firstprivate(a) 
		#pragma cetus private(i, j, k) 
		#pragma cetus lastprivate(a) 
		#pragma cetus parallel 
		#pragma omp parallel for private(i, j, k) firstprivate(a) lastprivate(a)
		for (k=0; k<200; k ++ )
		{
			#pragma loop name main#6#0 
			#pragma cetus private(i, j) 
			for (i=1; i<(1024+1); i ++ )
			{
				#pragma loop name main#6#0#0 
				#pragma cetus private(j) 
				for (j=1; j<(1024+1); j ++ )
				{
					a[i][j]=((((b[i-1][j]+b[i+1][j])+b[i][j-1])+b[i][j+1])/4);
				}
			}
			#pragma loop name main#6#1 
			#pragma cetus private(i, j) 
			for (i=1; i<(1024+1); i ++ )
			{
				#pragma loop name main#6#1#0 
				#pragma cetus private(j) 
				for (j=1; j<(1024+1); j ++ )
				{
					b[i][j]=a[i][j];
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
		#pragma cetus firstprivate(a) 
		#pragma cetus private(i, j, k, kk) 
		#pragma cetus lastprivate(a) 
		#pragma cetus parallel 
		#pragma omp parallel for private(i, j, k, kk) firstprivate(a) lastprivate(a)
		for ((kk=0); kk<200; kk+=kTile)
		{
			#pragma loop name main#7#0 
			#pragma cetus firstprivate(a) 
			#pragma cetus private(i, j, k) 
			#pragma cetus lastprivate(a) 
			for ((k=kk); k<(((kTile+kk)<200) ? (kTile+kk) : 200); k ++ )
			{
				#pragma loop name main#7#0#0 
				#pragma cetus private(i, j) 
				for (i=1; i<(1024+1); i ++ )
				{
					#pragma loop name main#7#0#0#0 
					#pragma cetus private(j) 
					for (j=1; j<(1024+1); j ++ )
					{
						a[i][j]=((((b[i-1][j]+b[i+1][j])+b[i][j-1])+b[i][j+1])/4);
					}
				}
				#pragma loop name main#7#0#1 
				#pragma cetus private(i, j) 
				for (i=1; i<(1024+1); i ++ )
				{
					#pragma loop name main#7#0#1#0 
					#pragma cetus private(j) 
					for (j=1; j<(1024+1); j ++ )
					{
						b[i][j]=a[i][j];
					}
				}
			}
		}
	}
	_ret_val_0=0;
	return _ret_val_0;
}
