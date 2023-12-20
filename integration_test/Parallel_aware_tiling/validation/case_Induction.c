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
Examples of Loops with Induction Variables



  The first loop includes a basic, linear induction variable ind.  



  The second loop includes a more generalized induction variable, which uses a

  linear induction variable as the increment.




*/
int main()
{
	float a[10000], b[10000];
	int i, n, ind, ind2;
	int _ret_val_0;
	int cores = 4;
	int cacheSize = 8192;
	n=10000;
	ind=123;
	if ((n<=100000)&&(cacheSize>159984))
	{
		#pragma loop name main#0 
		#pragma cetus private(i) 
		#pragma cetus parallel 
		#pragma omp parallel for private(i)
		for (i=1; i<n; i ++ )
		{
			a[123+(2*i)]=b[i];
		}
	}
	else
	{
		int balancedTileSize = ((cacheSize/4)/cores);
		#pragma loop name main#1 
		#pragma cetus private(i) 
		#pragma cetus parallel 
		#pragma omp parallel for private(i)
		for (i=1; i<n; i ++ )
		{
			a[123+(2*i)]=b[i];
		}
	}
	ind+=19998;
	ind2=5;
	ind=234;
	if ((n<=100000)&&(cacheSize>(-16+(16*n))))
	{
		#pragma loop name main#2 
		#pragma cetus private(i) 
		#pragma cetus parallel 
		#pragma omp parallel for private(i)
		for (i=1; i<n; i ++ )
		{
			a[(5+(235*i))+(i*i)]=b[i];
		}
	}
	else
	{
		int balancedTileSize = ((cacheSize/4)/cores);
		#pragma loop name main#3 
		#pragma cetus private(i) 
		#pragma cetus parallel 
		#pragma omp parallel for private(i)
		for (i=1; i<n; i ++ )
		{
			a[(5+(235*i))+(i*i)]=b[i];
		}
	}
	ind2+=((-234+(233*n))+(n*n));
	ind+=(-2+(2*n));
	_ret_val_0=0;
	return _ret_val_0;
}
