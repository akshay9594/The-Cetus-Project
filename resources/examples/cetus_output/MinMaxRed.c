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
int x = 1;
float y = 0.0;
int main()
{
	int a[10000], c[10000];
	int b, i, maxl, minl, d, e;
	int x1, x2, t1, t2, t3, t4, l, sx, sy;
	int j;
	int _ret_val_0;
	b=1;
	maxl=1;
	minl=1000;
	#pragma cetus private(i) 
	#pragma loop name main#0 
	#pragma cetus reduction(max: maxl) reduction(&: b) reduction(*: e) reduction(+: d) 
	#pragma cetus parallel 
	#pragma omp parallel for private(i) reduction(max: maxl)reduction(&: b)reduction(*: e)reduction(+: d)
	for (i=0; i<10000; i ++ )
	{
		b&=a[i];
		d+=a[i];
		e*=a[i];
		maxl=((maxl>a[i]) ? maxl : a[i]);
	}
	#pragma cetus private(j) 
	#pragma loop name main#1 
	#pragma cetus reduction(max: maxl) 
	#pragma cetus parallel 
	#pragma omp parallel for private(j) reduction(max: maxl)
	for (j=0; j<10000; j ++ )
	{
		if (a[j]>maxl)
		{
			maxl=a[j];
		}
		/* c[i] = minl; */
	}
	#pragma cetus private(i) 
	#pragma loop name main#2 
	#pragma cetus reduction(min: minl) 
	#pragma cetus parallel 
	#pragma omp parallel for private(i) reduction(min: minl)
	for (i=0; i<10000; i ++ )
	{
		minl=((minl<a[i]) ? minl : a[i]);
	}
	_ret_val_0=0;
	return _ret_val_0;
}
