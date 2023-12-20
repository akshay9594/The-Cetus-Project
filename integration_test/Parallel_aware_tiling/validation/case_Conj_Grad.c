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


	Computationally intensive loops extracted from the CG benchmark 

	of the NAS Parallel Benchmarks - C version (SNU NPB- 1.0.3)


*/
int main()
{
	int colidx[15000], rowstr[15000];
	double x[15000], z[15000], a[15000], p[15000], q[15000], r[15000], * rnorm;
	int firstrow = 1, lastrow = 15000, firstcol = 1, lastcol = 15000;
	int j, k;
	int cgit, cgitmax = 25;
	double d, sum, rho, rho0, alpha, beta;
	int _ret_val_0;
	int cores = 4;
	int cacheSize = 8192;
	rho=0.0;
	/* --------------------------------------------------------------------- */
	/* Initialize the CG algorithm: */
	/* --------------------------------------------------------------------- */
	if (((15000+1)<=100000)&&(cacheSize>240000))
	{
		#pragma loop name main#0 
		#pragma cetus private(j) 
		#pragma cetus parallel 
		#pragma omp parallel for private(j)
		for (j=0; j<(15000+1); j ++ )
		{
			q[j]=0.0;
			z[j]=0.0;
			r[j]=x[j];
			p[j]=r[j];
		}
	}
	else
	{
		int balancedTileSize = ((cacheSize/8)/cores);
		#pragma loop name main#1 
		#pragma cetus private(j) 
		#pragma cetus parallel 
		#pragma omp parallel for private(j)
		for (j=0; j<(15000+1); j ++ )
		{
			q[j]=0.0;
			z[j]=0.0;
			r[j]=x[j];
			p[j]=r[j];
		}
	}
	/* --------------------------------------------------------------------- */
	/* rho = r.r */
	/* Now, obtain the norm of r: First, sum squares of r elements locally... */
	/* --------------------------------------------------------------------- */
	if ((((lastcol-firstcol)+1)<=100000)&&(cacheSize>239984))
	{
		#pragma loop name main#2 
		#pragma cetus private(j) 
		#pragma cetus reduction(+: rho) 
		#pragma cetus parallel 
		#pragma omp parallel for private(j) reduction(+: rho)
		for (j=0; j<((lastcol-firstcol)+1); j ++ )
		{
			rho=(rho+(r[j]*r[j]));
		}
	}
	else
	{
		int balancedTileSize = ((cacheSize/8)/cores);
		#pragma loop name main#3 
		#pragma cetus private(j) 
		#pragma cetus reduction(+: rho) 
		#pragma cetus parallel 
		#pragma omp parallel for private(j) reduction(+: rho)
		for (j=0; j<((lastcol-firstcol)+1); j ++ )
		{
			rho=(rho+(r[j]*r[j]));
		}
	}
	/* --------------------------------------------------------------------- */
	/* ----> */
	/* The conj grad iteration loop */
	/* ----> */
	/* --------------------------------------------------------------------- */
	#pragma loop name main#2 
	#pragma cetus private(alpha, beta, cgit, d, j, k, q, rho0, sum) 
	/* #pragma cetus reduction(+: z[j])  */
	#pragma loop name main#4 
	#pragma cetus private(alpha, beta, cgit, d, j, k, q, rho0, sum) 
	/* #pragma cetus reduction(+: z[j])  */
	for (cgit=1; cgit<=cgitmax; cgit ++ )
	{
		/* --------------------------------------------------------------------- */
		/* q = A.p */
		/* The partition submatrix-vector multiply: use workspace w */
		/* --------------------------------------------------------------------- */
		/*  */
		/* NOTE: this version of the multiply is actually (slightly: maybe %5)  */
		/*       faster on the sp2 on 16 nodes than is the unrolled-by-2 version  */
		/*       below.   On the Cray t3d, the reverse is true, i.e., the  */
		/*       unrolled-by-two version is some 10% faster.   */
		/*       The unrolled-by-8 version below is significantly faster */
		/*       on the Cray t3d - overall speed of code is 1.5 times faster. */
		#pragma loop name main#2#0 
		#pragma cetus private(j, k, sum) 
		#pragma cetus parallel 
		#pragma loop name main#4#0 
		#pragma cetus private(j, k, sum) 
		#pragma cetus parallel 
		/* #pragma omp parallel for private(j, k, sum) */
		#pragma omp parallel for private(j, k, sum)
		for (j=0; j<((lastrow-firstrow)+1); j ++ )
		{
			sum=0.0;
			#pragma loop name main#2#0#0 
			#pragma cetus private(k) 
			/* #pragma cetus reduction(+: sum)  */
			#pragma loop name main#4#0#0 
			#pragma cetus private(k) 
			/* #pragma cetus reduction(+: sum)  */
			for (k=rowstr[j]; k<rowstr[j+1]; k ++ )
			{
				sum=(sum+(a[k]*p[colidx[k]]));
			}
			q[j]=sum;
		}
		/* --------------------------------------------------------------------- */
		/* Obtain p.q */
		/* --------------------------------------------------------------------- */
		d=0.0;
		#pragma loop name main#2#1 
		#pragma cetus private(j) 
		#pragma cetus reduction(+: d) 
		#pragma cetus parallel 
		#pragma loop name main#4#1 
		#pragma cetus private(j) 
		#pragma cetus reduction(+: d) 
		#pragma cetus parallel 
		/* #pragma omp parallel for private(j) reduction(+: d) */
		#pragma omp parallel for private(j) reduction(+: d)
		for (j=0; j<((lastcol-firstcol)+1); j ++ )
		{
			d=(d+(p[j]*q[j]));
		}
		/* --------------------------------------------------------------------- */
		/* Obtain alpha = rho (p.q) */
		/* --------------------------------------------------------------------- */
		alpha=(rho/d);
		/* --------------------------------------------------------------------- */
		/* Save a temporary of rho */
		/* --------------------------------------------------------------------- */
		rho0=rho;
		/* --------------------------------------------------------------------- */
		/* Obtain z = z + alphap */
		/* and    r = r - alphaq */
		/* --------------------------------------------------------------------- */
		rho=0.0;
		#pragma loop name main#2#2 
		#pragma cetus private(j) 
		#pragma cetus parallel 
		#pragma loop name main#4#2 
		#pragma cetus private(j) 
		#pragma cetus parallel 
		/* #pragma omp parallel for if((10000<(60005L+(-4Lfirstcol)))) private(j) */
		#pragma omp parallel for private(j)
		for (j=0; j<((lastcol-firstcol)+1); j ++ )
		{
			z[j]=(z[j]+(alpha*p[j]));
			r[j]=(r[j]-(alpha*q[j]));
		}
		/* --------------------------------------------------------------------- */
		/* rho = r.r */
		/* Now, obtain the norm of r: First, sum squares of r elements locally... */
		/* --------------------------------------------------------------------- */
		#pragma loop name main#2#3 
		#pragma cetus private(j) 
		#pragma cetus reduction(+: rho) 
		#pragma cetus parallel 
		#pragma loop name main#4#3 
		#pragma cetus private(j) 
		#pragma cetus reduction(+: rho) 
		#pragma cetus parallel 
		/* #pragma omp parallel for private(j) reduction(+: rho) */
		#pragma omp parallel for private(j) reduction(+: rho)
		for (j=0; j<((lastcol-firstcol)+1); j ++ )
		{
			rho=(rho+(r[j]*r[j]));
		}
		/* --------------------------------------------------------------------- */
		/* Obtain beta: */
		/* --------------------------------------------------------------------- */
		beta=(rho/rho0);
		/* --------------------------------------------------------------------- */
		/* p = r + betap */
		/* --------------------------------------------------------------------- */
		#pragma loop name main#2#4 
		#pragma cetus private(j) 
		#pragma cetus parallel 
		#pragma loop name main#4#4 
		#pragma cetus private(j) 
		#pragma cetus parallel 
		/* #pragma omp parallel for if((10000<(45004L+(-3Lfirstcol)))) private(j) */
		#pragma omp parallel for private(j)
		for (j=0; j<((lastcol-firstcol)+1); j ++ )
		{
			p[j]=(r[j]+(beta*p[j]));
		}
	}
	/* end of do cgit=1,cgitmax */
	/* --------------------------------------------------------------------- */
	/* Compute residual norm explicitly:  ||r|| = ||x - A.z|| */
	/* First, form A.z */
	/* The partition submatrix-vector multiply */
	/* --------------------------------------------------------------------- */
	sum=0.0;
	#pragma loop name main#3 
	#pragma cetus private(d, j, k) 
	#pragma cetus parallel 
	#pragma loop name main#5 
	#pragma cetus private(d, j, k) 
	#pragma cetus parallel 
	/* #pragma omp parallel for private(d, j, k) */
	#pragma omp parallel for private(d, j, k)
	for (j=0; j<((lastrow-firstrow)+1); j ++ )
	{
		d=0.0;
		#pragma loop name main#3#0 
		#pragma cetus private(k) 
		/* #pragma cetus reduction(+: d)  */
		#pragma loop name main#5#0 
		#pragma cetus private(k) 
		/* #pragma cetus reduction(+: d)  */
		for (k=rowstr[j]; k<rowstr[j+1]; k ++ )
		{
			d=(d+(a[k]*z[colidx[k]]));
		}
		r[j]=d;
	}
	/* --------------------------------------------------------------------- */
	/* At this point, r contains A.z */
	/* --------------------------------------------------------------------- */
	if ((((lastcol-firstcol)+1)<=100000)&&(cacheSize>239984))
	{
		#pragma loop name main#6 
		#pragma cetus private(d, j) 
		#pragma cetus reduction(+: sum) 
		#pragma cetus parallel 
		#pragma omp parallel for private(d, j) reduction(+: sum)
		for (j=0; j<((lastcol-firstcol)+1); j ++ )
		{
			d=(x[j]-r[j]);
			sum=(sum+(d*d));
		}
	}
	else
	{
		int balancedTileSize = ((cacheSize/8)/cores);
		#pragma loop name main#7 
		#pragma cetus private(d, j) 
		#pragma cetus reduction(+: sum) 
		#pragma cetus parallel 
		#pragma omp parallel for private(d, j) reduction(+: sum)
		for (j=0; j<((lastcol-firstcol)+1); j ++ )
		{
			d=(x[j]-r[j]);
			sum=(sum+(d*d));
		}
	}
	return _ret_val_0;
}
