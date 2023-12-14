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
	int idx = 0, row_val[30000], nonzeros, col_ind[30000], row[30000], row_ptr[30000], n_rows;
	double nnz_val[30000], p[30000], W[30000], H[30000];
	int holder, i, k;
	int r, ind, t;
	double sm;
	int _ret_val_0;
	int ind_0;
	row[0]=0;
	holder=0;
	r=row_val[idx];
	#pragma cetus private(i) 
	#pragma loop name main#0 
	for (i=0; i<nonzeros; i ++ )
	{
		if (row_val[i]!=r)
		{
			row_ptr[holder ++ ]=i;
			r=row_val[i];
		}
	}
	/* row_ptr[holder+1] = nonzeros-1; */
	/* #pragma omp parallel for private(sm,r,ind,t) */
	#pragma cetus private(ind, ind_0, r, sm, t) 
	#pragma loop name main#1 
	#pragma cetus parallel 
	#pragma omp parallel for if(((-1+n_rows)<=holder)) private(ind, ind_0, r, sm, t)
	for (r=0; r<n_rows;  ++ r)
	{
		/* Normalized Loop */
		#pragma cetus private(sm, t) 
		#pragma cetus lastprivate(ind_0) 
		#pragma loop name main#1#0 
		for (ind_0=0; ind_0<=((-1+row_ptr[1+r])+(-1*row_ptr[r])); ind_0 ++ )
		{
			sm=0;
			#pragma cetus private(t) 
			#pragma loop name main#1#0#0 
			/* #pragma cetus reduction(+: sm)  */
			for (t=0; t<k;  ++ t)
			{
				sm+=(W[(r*k)+t]*H[(col_ind[ind_0+row_ptr[r]]*k)+t]);
			}
			p[ind_0+row_ptr[r]]=(sm*nnz_val[ind_0+row_ptr[r]]);
			/* Scaling of non-zero elements of the sparse matrix */
		}
		ind=(ind_0+row_ptr[r]);
	}
	_ret_val_0=0;
	return _ret_val_0;
}
