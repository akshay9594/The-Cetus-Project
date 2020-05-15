/*
Copyright (C) 1991-2018 Free Software Foundation, Inc.
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
   <http:www.gnu.org/licenses/>. 
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
/* We do not support C11 <threads.h>.  */
/* #include "stdio.h" */
/* #include<stdlib.h> */
/* #include "omp.h" */
int x = 1;
float y = 0.0;
int c[1000][1000];
int myArray[10000];
int p[1000][1000], b[1000][1000];
int main()
{
	int LX1, idel[2400][6][6][6], ntemp, iel;
	double a;
	int j, k;
	int i;
	int _ret_val_0;
	LX1=5;
	#pragma cetus private(a, i) 
	#pragma loop name main#0 
	#pragma cetus reduction(+: myArray[n]) 
	#pragma cetus parallel 
	#pragma omp parallel for private(a, i) reduction(+: myArray[n])
	for (i=0; i<10000; i ++ )
	{
		int n;
		a=2.0;
		/* Or something non-trivial justifying the parallelism... */
		#pragma cetus private(n) 
		#pragma loop name main#0#0 
		#pragma cetus parallel 
		#pragma omp parallel for private(n)
		for (n=0; n<10000; n ++ )
		{
			myArray[n]+=a;
		}
	}
	#pragma cetus private(i, j, k) 
	#pragma loop name main#1 
	#pragma cetus parallel 
	#pragma omp parallel for private(i, j, k)
	for (i=0; i<1000; i ++ )
	{
		#pragma cetus private(j, k) 
		#pragma loop name main#1#0 
		#pragma cetus reduction(+: c[i][j+1]) 
		#pragma cetus parallel 
		#pragma omp parallel for private(j, k) reduction(+: c[i][j+1])
		for (j=0; j<1000; j ++ )
		{
			#pragma cetus private(k) 
			#pragma loop name main#1#0#0 
			#pragma cetus reduction(+: c[i][j+1], c[i][j]) 
			#pragma cetus parallel 
			#pragma omp parallel for private(k) reduction(+: c[i][j+1], c[i][j])
			for (k=0; k<10000; k ++ )
			{
				c[i][j]+=(p[i][k]*b[k][j]);
				c[i][j+1]+=(p[i][k]*b[k][j+1]);
			}
		}
	}
	_ret_val_0=0;
	return _ret_val_0;
}
