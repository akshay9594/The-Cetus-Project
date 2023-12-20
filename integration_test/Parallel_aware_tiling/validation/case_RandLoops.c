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
int main()
{
	int _ret_val_0;
	_ret_val_0=0;
	return _ret_val_0;
}

int foo3()
{
	double a[10000][10000];
	int i, j;
	int _ret_val_0;
	int cores = 4;
	int cacheSize = 8192;
	if ((99970002<=100000)&&(cacheSize>159968))
	{
		#pragma loop name foo3#0 
		#pragma cetus private(i, j) 
		#pragma cetus parallel 
		#pragma omp parallel for private(i, j)
		for (i=0; i<=9998; i ++ )
		{
			#pragma loop name foo3#0#0 
			#pragma cetus private(j) 
			for (j=0; j<=9999; j ++ )
			{
				a[i][j]+=a[i+1][j];
			}
		}
	}
	else
	{
		int balancedTileSize = ((cacheSize/8)/cores);
		int jj;
		int jTile = balancedTileSize;
		#pragma loop name foo3#1 
		#pragma cetus private(i, j, jj) 
		#pragma cetus parallel 
		#pragma omp parallel for private(i, j, jj)
		for ((jj=0); jj<=9999; jj+=jTile)
		{
			#pragma loop name foo3#1#0 
			#pragma cetus private(i, j) 
			for (i=0; i<=9998; i ++ )
			{
				#pragma loop name foo3#1#0#0 
				#pragma cetus private(j) 
				for ((j=jj); j<=(((jTile+jj)<9999) ? (jTile+jj) : 9999); j ++ )
				{
					a[i][j]+=a[i+1][j];
				}
			}
		}
	}
	_ret_val_0=0;
	return _ret_val_0;
}

void foo4(int x, int y)
{
	double a[10000][10000];
	int i, j;
	int cores = 4;
	int cacheSize = 8192;
	if ((99970002<=100000)&&(cacheSize>159968))
	{
		#pragma loop name foo4#0 
		#pragma cetus private(i, j) 
		#pragma cetus parallel 
		#pragma omp parallel for private(i, j)
		for (i=0; i<=9998; i ++ )
		{
			#pragma loop name foo4#0#0 
			#pragma cetus private(j) 
			for (j=0; j<=9999; j ++ )
			{
				a[i][j]+=a[i+1][j];
			}
		}
	}
	else
	{
		int balancedTileSize = ((cacheSize/8)/cores);
		int jj;
		int jTile = balancedTileSize;
		#pragma loop name foo4#1 
		#pragma cetus private(i, j, jj) 
		#pragma cetus parallel 
		#pragma omp parallel for private(i, j, jj)
		for ((jj=0); jj<=9999; jj+=jTile)
		{
			#pragma loop name foo4#1#0 
			#pragma cetus private(i, j) 
			for (i=0; i<=9998; i ++ )
			{
				#pragma loop name foo4#1#0#0 
				#pragma cetus private(j) 
				for ((j=jj); j<=(((jTile+jj)<9999) ? (jTile+jj) : 9999); j ++ )
				{
					a[i][j]+=a[i+1][j];
				}
			}
		}
	}
	return ;
}
