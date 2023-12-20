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
Examples


*/
int main()
{
	int a[10], b[10], c[10], d[100000000][100000000];
	int k;
	int i;
	int j;
	int _ret_val_0;
	int cores = 4;
	int cacheSize = 8192;
	if ((10<=100000)&&(cacheSize>144))
	{
		#pragma loop name main#0 
		#pragma cetus private(k) 
		#pragma cetus parallel 
		#pragma omp parallel for private(k)
		for (k=0; k<10; k ++ )
		{
			a[k]=k;
			b[k]=(k-10);
			c[k]=1;
		}
	}
	else
	{
		int balancedTileSize = ((cacheSize/4)/cores);
		#pragma loop name main#1 
		#pragma cetus private(k) 
		#pragma cetus parallel 
		#pragma omp parallel for private(k)
		for (k=0; k<10; k ++ )
		{
			a[k]=k;
			b[k]=(k-10);
			c[k]=1;
		}
	}
	/* Flow dependence */
	if ((10000<=100000)&&(cacheSize>159984))
	{
		#pragma loop name main#2 
		#pragma cetus private(i) 
		#pragma cetus parallel 
		#pragma omp parallel for private(i)
		for (i=1; i<10000; i ++ )
		{
			a[i]=b[i];
			c[i]=a[i-1];
		}
	}
	else
	{
		int balancedTileSize = ((cacheSize/4)/cores);
		#pragma loop name main#3 
		#pragma cetus private(i) 
		#pragma cetus parallel 
		#pragma omp parallel for private(i)
		for (i=1; i<10000; i ++ )
		{
			a[i]=b[i];
			c[i]=a[i-1];
		}
	}
	if ((10000<=100000)&&(cacheSize>159984))
	{
		#pragma loop name main#4 
		#pragma cetus private(i) 
		#pragma cetus parallel 
		#pragma omp parallel for private(i)
		for (i=1; i<10000; i ++ )
		{
			a[i]=b[i];
			c[i]=(a[i]+b[i-1]);
		}
	}
	else
	{
		int balancedTileSize = ((cacheSize/4)/cores);
		#pragma loop name main#5 
		#pragma cetus private(i) 
		#pragma cetus parallel 
		#pragma omp parallel for private(i)
		for (i=1; i<10000; i ++ )
		{
			a[i]=b[i];
			c[i]=(a[i]+b[i-1]);
		}
	}
	/* Antidependence */
	if ((10000<=100000)&&(cacheSize>159984))
	{
		#pragma loop name main#6 
		#pragma cetus private(i) 
		#pragma cetus parallel 
		#pragma omp parallel for private(i)
		for (i=1; i<10000; i ++ )
		{
			a[i-1]=b[i];
			c[i]=a[i];
		}
	}
	else
	{
		int balancedTileSize = ((cacheSize/4)/cores);
		#pragma loop name main#7 
		#pragma cetus private(i) 
		#pragma cetus parallel 
		#pragma omp parallel for private(i)
		for (i=1; i<10000; i ++ )
		{
			a[i-1]=b[i];
			c[i]=a[i];
		}
	}
	/* Output dependence */
	if ((10000<=100000)&&(cacheSize>159984))
	{
		#pragma loop name main#8 
		#pragma cetus private(i) 
		#pragma cetus parallel 
		#pragma omp parallel for private(i)
		for (i=1; i<10000; i ++ )
		{
			a[i]=b[i];
			a[i+1]=c[i];
		}
	}
	else
	{
		int balancedTileSize = ((cacheSize/4)/cores);
		#pragma loop name main#9 
		#pragma cetus private(i) 
		#pragma cetus parallel 
		#pragma omp parallel for private(i)
		for (i=1; i<10000; i ++ )
		{
			a[i]=b[i];
			a[i+1]=c[i];
		}
	}
	if ((100000000<=100000)&&(cacheSize>159984))
	{
		#pragma loop name main#10 
		#pragma cetus private(i, j) 
		#pragma cetus parallel 
		#pragma omp parallel for private(i, j)
		for (i=0; i<10000; i ++ )
		{
			#pragma loop name main#10#0 
			#pragma cetus private(j) 
			for (j=0; j<10000; j ++ )
			{
				d[i][j]=(i+j);
			}
		}
	}
	else
	{
		int balancedTileSize = ((cacheSize/4)/cores);
		int jj;
		int jTile = balancedTileSize;
		#pragma loop name main#11 
		#pragma cetus private(i, j, jj) 
		#pragma cetus parallel 
		#pragma omp parallel for private(i, j, jj)
		for ((jj=0); jj<10000; jj+=jTile)
		{
			#pragma loop name main#11#0 
			#pragma cetus private(i, j) 
			for (i=0; i<10000; i ++ )
			{
				#pragma loop name main#11#0#0 
				#pragma cetus private(j) 
				for ((j=jj); j<(((jTile+jj)<10000) ? (jTile+jj) : 10000); j ++ )
				{
					d[i][j]=(i+j);
				}
			}
		}
	}
	/* loop interchange */
	if ((100000000<=100000)&&(cacheSize>159984))
	{
		#pragma loop name main#12 
		#pragma cetus private(i, j) 
		#pragma cetus parallel 
		#pragma omp parallel for private(i, j)
		for (i=0; i<10000; i ++ )
		{
			#pragma loop name main#12#0 
			#pragma cetus private(j) 
			for (j=0; j<10000; j ++ )
			{
				d[i+1][j+2]=(d[i][j]+1);
			}
		}
	}
	else
	{
		int balancedTileSize = ((cacheSize/4)/cores);
		int jj;
		int jTile = balancedTileSize;
		#pragma loop name main#13 
		#pragma cetus private(i, j, jj) 
		#pragma cetus parallel 
		#pragma omp parallel for private(i, j, jj)
		for ((jj=0); jj<10000; jj+=jTile)
		{
			#pragma loop name main#13#0 
			#pragma cetus private(i, j) 
			for (i=0; i<10000; i ++ )
			{
				#pragma loop name main#13#0#0 
				#pragma cetus private(j) 
				for ((j=jj); j<(((jTile+jj)<10000) ? (jTile+jj) : 10000); j ++ )
				{
					d[i+1][j+2]=(d[i][j]+1);
				}
			}
		}
	}
	if ((100000000<=100000)&&(cacheSize>159984))
	{
		#pragma loop name main#14 
		#pragma cetus private(i, j) 
		#pragma cetus parallel 
		#pragma omp parallel for private(i, j)
		for (i=0; i<10000; i ++ )
		{
			#pragma loop name main#14#0 
			#pragma cetus private(j) 
			for (j=0; j<10000; j ++ )
			{
				d[i][j+2]=(d[i][j]+1);
			}
		}
	}
	else
	{
		int balancedTileSize = ((cacheSize/4)/cores);
		int jj;
		int jTile = balancedTileSize;
		#pragma loop name main#15 
		#pragma cetus private(i, j, jj) 
		#pragma cetus parallel 
		#pragma omp parallel for private(i, j, jj)
		for ((jj=0); jj<10000; jj+=jTile)
		{
			#pragma loop name main#15#0 
			#pragma cetus private(i, j) 
			for (i=0; i<10000; i ++ )
			{
				#pragma loop name main#15#0#0 
				#pragma cetus private(j) 
				for ((j=jj); j<(((jTile+jj)<10000) ? (jTile+jj) : 10000); j ++ )
				{
					d[i][j+2]=(d[i][j]+1);
				}
			}
		}
	}
	if ((100000000<=100000)&&(cacheSize>159984))
	{
		#pragma loop name main#16 
		#pragma cetus private(i, j) 
		#pragma cetus parallel 
		#pragma omp parallel for private(i, j)
		for (i=0; i<10000; i ++ )
		{
			#pragma loop name main#16#0 
			#pragma cetus private(j) 
			for (j=0; j<10000; j ++ )
			{
				d[i+1][j-2]=(d[i][j]+1);
			}
		}
	}
	else
	{
		int balancedTileSize = ((cacheSize/4)/cores);
		int jj;
		int jTile = balancedTileSize;
		#pragma loop name main#17 
		#pragma cetus private(i, j, jj) 
		#pragma cetus parallel 
		#pragma omp parallel for private(i, j, jj)
		for ((jj=0); jj<10000; jj+=jTile)
		{
			#pragma loop name main#17#0 
			#pragma cetus private(i, j) 
			for (i=0; i<10000; i ++ )
			{
				#pragma loop name main#17#0#0 
				#pragma cetus private(j) 
				for ((j=jj); j<(((jTile+jj)<10000) ? (jTile+jj) : 10000); j ++ )
				{
					d[i+1][j-2]=(d[i][j]+1);
				}
			}
		}
	}
	_ret_val_0=0;
	return _ret_val_0;
}
