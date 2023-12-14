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

 Subscripted subscript example from the Algebraic Multigrid kernel (Amgmk)

*/
#include <stdio.h>
#include <math.h>
int main()
{
	int i, j, jj, irownnz, m, adiag, vecstride_y, idxstride_y, vecstride_x, idxstride_x;
	int num_vectors, num_rownnz, num_rows = 30000;
	int A_i[30000], Arownnz[30000], y_data[30000], A_data[30000], A_j[30000], x_data[30000];
	int _ret_val_0;
	int jj_0;
	irownnz=0;
	#pragma cetus private(adiag, i) 
	#pragma loop name main#0 
	for (i=0; i<num_rows; i ++ )
	{
		adiag=(A_i[i+1]-A_i[i]);
		if (adiag>0)
		{
			Arownnz[irownnz ++ ]=i;
		}
	}
	/* Loop to parallelize */
	#pragma cetus private(i, j, jj, jj_0, m) 
	#pragma loop name main#1 
	/* #pragma cetus reduction(+: y_data[m])  */
	#pragma cetus parallel 
	#pragma omp parallel for if(((-1+num_rownnz)<=irownnz)) private(i, j, jj, jj_0, m)
	for (i=0; i<num_rownnz; i ++ )
	{
		m=Arownnz[i];
		/* Normalized Loop */
		#pragma cetus private(j) 
		#pragma cetus lastprivate(jj_0) 
		#pragma loop name main#1#0 
		/* #pragma cetus reduction(+: y_data[m])  */
		for (jj_0=0; jj_0<=((-1+A_i[1+m])+(-1*A_i[Arownnz[i]])); jj_0 ++ )
		{
			j=A_j[jj_0+A_i[Arownnz[i]]];
			y_data[m]+=(A_data[jj_0+A_i[Arownnz[i]]]*x_data[j]);
		}
		jj=(jj_0+A_i[Arownnz[i]]);
	}
	/* Loop to parallelize */
	/*   for (i = 0; i < num_rownnz; i++) */
	/*  { */
		/*        m = Arownnz[i];       */
		/*         for ( j=0; j<num_vectors; ++j) */
		/*         { */
			/*             for (jj = A_i[m]; jj < A_i[m+1]; jj++)  */
			/*               y_data[ jvecstride_y + m*idxstride_y ] +=  A_data[jj] * x_data[ j*vecstride_x + A_j[jj]*idxstride_x ]; */
		/*         } */
	/*  } */
	_ret_val_0=0;
	return _ret_val_0;
}
