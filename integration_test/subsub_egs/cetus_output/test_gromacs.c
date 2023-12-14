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
 
    Subscripted Subscript example from Gromacs - SPEC CPU 2006

*/
#include <stdio.h>
#include <math.h>
#include <stdlib.h>
int main()
{
	int bla1[30000], bla2[30000], Z[30000], blm[30000], blcc[30000], rhs1[30000], iatom[((3*30000)+2)];
	int sol[30000], bllen[30000], blnr[30000], r[30000][3], q[30000], xp[30000][3], X[30000], Y[30000][30000];
	int i, j, k, n, b, tmp0, tmp1, tmp2, mvb;
	int a1, a2, start;
	int _ret_val_0;
	int n_0;
	blnr[0]=0;
	#pragma cetus private(a1, a2, i, k) 
	#pragma loop name main#0 
	for (i=0; i<30000; i ++ )
	{
		a1=iatom[(3*i)+1];
		a2=iatom[(3*i)+2];
		blnr[i+1]=blnr[i];
		#pragma cetus private(k) 
		#pragma loop name main#0#0 
		for (k=0; k<10000; k ++ )
		{
			if (Y[a1-start][k]!=i)
			{
				Z[blnr[i+1] ++ ]=Y[a1-start][k];
			}
		}
		#pragma cetus private(k) 
		#pragma loop name main#0#1 
		for (k=0; k<10000; k ++ )
		{
			if (Y[a2-start][k]!=i)
			{
				Z[blnr[i+1] ++ ]=Y[a2-start][k];
			}
		}
	}
	/* loop to parallelize */
	#pragma cetus private(b, i, j, k, mvb, n, n_0, tmp0, tmp1, tmp2) 
	#pragma loop name main#1 
	#pragma cetus parallel 
	#pragma omp parallel for if((29999<=(i+1))) private(b, i, j, k, mvb, n, n_0, tmp0, tmp1, tmp2)
	for (b=0; b<30000; b ++ )
	{
		tmp0=r[b][0];
		tmp1=r[b][1];
		tmp2=r[b][2];
		i=bla1[b];
		j=bla2[b];
		/* Normalized Loop */
		#pragma cetus private(k) 
		#pragma cetus lastprivate(n_0) 
		#pragma loop name main#1#0 
		for (n_0=0; n_0<=((-1+blnr[1+b])+(-1*blnr[b])); n_0 ++ )
		{
			k=Z[n_0+blnr[b]];
			blm[n_0+blnr[b]]=(blcc[n_0+blnr[b]]*(((tmp0*r[k][0])+(tmp1*r[k][1]))+(tmp2*r[k][2])));
		}
		n=(n_0+blnr[b]);
		mvb=(q[b]*((((tmp0*(xp[i][0]-xp[j][0]))+(tmp1*(xp[i][1]-xp[j][1])))+(tmp2*(xp[i][2]-xp[j][2])))-bllen[b]));
		rhs1[b]=mvb;
		sol[b]=mvb;
	}
	_ret_val_0=0;
	return _ret_val_0;
}
