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
/*
Scalar, Additive Reduction

  The loop contains a scalar, additive reduction operation.


*/
int main()
{
	float a[10000], max_val, sum;
	int i, n;
	int _ret_val_0;
	sum=0.0;
	n=10000;
	#pragma cetus private(i) 
	#pragma loop name main#0 
	#pragma cetus parallel 
	#pragma omp parallel for private(i)
	for (i=0; i<10000; i ++ )
	{
		a[i]=(i/2);
	}
	#pragma cetus private(i) 
	#pragma loop name main#1 
	#pragma cetus reduction(+: sum) 
	#pragma cetus parallel 
	#pragma omp parallel for private(i) reduction(+: sum)
	for (i=0; i<10000; i ++ )
	{
		sum=(sum+a[i]);
	}
	#pragma cetus private(i) 
	#pragma loop name main#2 
	for (i=0; i<n; i ++ )
	{
		if (a[i]>max_val)
		{
			max_val=a[i];
		}
	}
	_ret_val_0=0;
	return _ret_val_0;
}
