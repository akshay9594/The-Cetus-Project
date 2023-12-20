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
 Matrix multiplication


*/
int main()
{
	int n = 1000, m = 1000;
	int a[n][n], b[n][m], d[n][m];
	int i, j, k;
	int _ret_val_0;
	int cores = 4;
	int cacheSize = 8192;
	if ((((m*n)*n)<=100000)&&(cacheSize>15984))
	{
		#pragma loop name main#0 
		#pragma cetus private(i, j, k) 
		#pragma cetus parallel 
		#pragma omp parallel for private(i, j, k)
		for (i=0; i<n; i ++ )
		{
			#pragma loop name main#0#0 
			#pragma cetus private(j, k) 
			for (j=0; j<m; j ++ )
			{
				#pragma loop name main#0#0#0 
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
		#pragma loop name main#1 
		#pragma cetus private(i, j, k, kk) 
		#pragma cetus parallel 
		#pragma omp parallel for private(i, j, k, kk)
		for (i=0; i<n; i ++ )
		{
			#pragma loop name main#1#0 
			#pragma cetus private(j, k, kk) 
			/* #pragma cetus reduction(+: d[i][j])  */
			for ((kk=0); kk<n; kk+=kTile)
			{
				#pragma loop name main#1#0#0 
				#pragma cetus private(j, k) 
				for (j=0; j<m; j ++ )
				{
					#pragma loop name main#1#0#0#0 
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
	_ret_val_0=0;
	return _ret_val_0;
}
