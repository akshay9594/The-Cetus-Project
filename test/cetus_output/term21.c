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
term21.f -- translated by f2c (version 20160102).
   You must link the resulting object file with libf2c:
	on Microsoft Windows system, link with libf2c.lib;
	on Linux or Unix systems, link with ...path/to/libf2c.a -lm
	or, if you install libf2c.a in a standard place, with -lf2c -lm
	-- in that order, at the end of the command line, as in
		cc *.o -lf2c -lm
	Source for libf2c is in /netlib/f2c/libf2c.zip, e.g.,

		http:www.netlib.org/f2c/libf2c.zip

*/
/* #include "f2c.h" */
/* Common Block Declarations */
struct named_term21_c_3
{
	double h__;
	int max__;
};

struct named_term21_c_3 radial_;
struct named_term21_c_20
{
	int nmax, lmax, nmax1, lmax1;
};

struct named_term21_c_20 ldata_;
struct named_term21_c_41
{
	double eo[100];
	int no[100], ko[100], ncore;
};

struct named_term21_c_41 core_;
struct named_term21_c_73
{
	/* was [100][100][100][100][100] */ 
	int ipxc[10000000000], icchan;
};

struct named_term21_c_73 irhoc_;
struct named_term21_c_91
{
	/* was [100][100][100] */ 
	double xci[1000000], rmi[10000];
};

/*

	    was [100][100]
*/
struct named_term21_c_91 rhocin_;
struct named_term21_c_112
{
	/* was [100][100][100] */ 
	double xco[1000000], rmo[10000];
};

/*

	    was [100][100]
*/
struct named_term21_c_112 rhocout_;
struct named_term21_c_133
{
	int iqa[100], iqb[100];
};

struct named_term21_c_133 iqqq_;
/* Table of constant values */
static int c_n1 =  - 1;
static int c__1 = 1;
static int c__11 = 11;
/* Subroutine */
int term21_(void )
{
	/* System generated locals */
	int i__1, i__2, i__3, i__4, i__5, i__6, i__7, i__8, i__9, i__10, i__11, i__12;
	/* Builtin functions */
	int pow_ii(int * , int * );
	/* Local variables */
	static double c__;
	static int i__, j, k, l, m, n;
	extern double s_(int * , int * , int * );
	static double u[100], v[100], c1;
	static int i1, i2, k1, m0, i5, i6, n0;
	/* was [100][100] */
	/*
	was [100][
		    100]
	*/
	static double v1[10000], v2[10000], z1;
	static int ja;
	/* was [100][100][100] */
	static double ff[1000000];
	static int ka;
	/* was [100][100][100] */
	static double gg[1000000];
	static int na, la, kb, nb, lb, jb, km, lm, jm, ij, kr, lr, jr, nr;
	static double rp[100];
	static int ks, ls, js, kn;
	/* Subroutine */
	extern int st_(int * , int * );
	static int ln, jn;
	/* was [100][100][100] */
	static double uu[1000000], vv[100];
	static int n0a, n0b;
	extern double d6j_(int * , int * , int * , int * , int * , int * );
	static int kk1, n0r, n0s;
	/* was [100][100][100][100] */
	/* was [100][100] */
	/*
	was [100][
		    100]
	*/
	/* was [100][100][100][100] */
	static double uu1[100000000], uv1[10000], uv2[10000], uu2[100000000];
	/* Subroutine */
	extern int odd_(int * , int * ), klj_(int * , int * , int * , int * , int * , int * );
	static int iiq, nnm, nnn, nnr, nns, nss, nxx, nhf1, inda, kapa, kapb, indb;
	static double cfin;
	static int indm, kapm, indn, kapn, indr, kapr, inds, kaps, kmin, kmax;
	/* Subroutine */
	extern int trgi_(int * , int * , int * , int * );
	static int nnmq;
	extern double rint_(double * , int * , int * , int * , double * );
	/* Subroutine */
	extern int yfun_(double * , double * , int * , int * );
	static int ittt;
	/* Subroutine */
	extern int indk1_(int * , int * , int * , int * , int * , int * );
	static int k1min, l1min, k1max, l1max, index, index1;
	/*      include "all.par" */
	int _ret_val_0;
	iiq=0;
	i__1=core_.ncore;
	#pragma loop name term21_#0 
	for (i1=1; i1<=i__1;  ++ i1)
	{
		i__2=core_.ncore;
		#pragma loop name term21_#0#0 
		for (i2=1; i2<=i__2;  ++ i2)
		{
			if (i1>=i2)
			{
				 ++ iiq;
				iqqq_.iqa[iiq-1]=i1;
				iqqq_.iqb[iiq-1]=i2;
				/*          write (,'(3i5)') i1,i2,iiq,nnh */
			}
			/* L110: */
		}
		/* L109: */
	}
	nnmq=iiq;
	i__1=nnmq;
	#pragma loop name term21_#1 
	for (ittt=1; ittt<=i__1;  ++ ittt)
	{
		i__=iqqq_.iqa[ittt-1];
		j=iqqq_.iqb[ittt-1];
		ka=core_.ko[i__-1];
		na=core_.no[i__-1];
		klj_( & ka,  & kapa,  & la,  & ja,  & inda,  & n0a);
		kb=core_.ko[j-1];
		nb=core_.no[j-1];
		klj_( & kb,  & kapb,  & lb,  & jb,  & indb,  & n0b);
		i__2=((ldata_.lmax<<1)+1);
		#pragma loop name term21_#1#0 
		for (indm=1; indm<=i__2;  ++ indm)
		{
			indk1_( & indm,  & km,  & kapm,  & lm,  & jm,  & m0);
			st_( & km,  & nnm);
			i__3=kk1;
			#pragma loop name term21_#1#0#0 
			for (indn=1; indn<=i__3;  ++ indn)
			{
				i__4=kk1;
				#pragma loop name term21_#1#0#0#0 
				for (k=0; k<=i__4;  ++ k)
				{
					i__5=nxx;
					#pragma loop name term21_#1#0#0#0#0 
					for (m=1; m<=i__5;  ++ m)
					{
						i__6=nhf1;
						#pragma loop name term21_#1#0#0#0#0#0 
						for (ij=1; ij<=i__6;  ++ ij)
						{
							uu1[(ij+((m+((k+(indn*100))*100))*100))-1010101]=0.0;
							uu2[(ij+((m+((k+(indn*100))*100))*100))-1010101]=0.0;
							/* L94: */
						}
						/* L93: */
					}
					/* L92: */
				}
				/* L91: */
			}
			i__3=((ldata_.lmax<<1)+1);
			#pragma loop name term21_#1#0#1 
			/* #pragma cetus reduction(+: uu1[((ij+((m+((k+(indn100))*100))*100))-1010101)], uu2[((ij+((m+((k+(indn*100))*100))*100))-1010101)])  */
			for (indr=1; indr<=i__3;  ++ indr)
			{
				indk1_( & indr,  & kr,  & kapr,  & lr,  & jr,  & n0r);
				st_( & kr,  & nnr);
				l1min=((i__4=(kapm-kapr)), abs(i__4));
				l1max=((kapm+kapr)-1);
				i__4=l1max;
				#pragma loop name term21_#1#0#1#0 
				/* #pragma cetus reduction(+: uu1[((ij+((m+((k+(indn100))*100))*100))-1010101)], uu2[((ij+((m+((k+(indn*100))*100))*100))-1010101)])  */
				for (l=l1min; l<=i__4;  ++ l)
				{
					i__5=((lm+lr)+l);
					odd_( & i__5,  & i5);
					if (i5==0)
					{
						goto L900;
					}
					i__5=ldata_.nmax;
					#pragma loop name term21_#1#0#1#0#0 
					for (m=nnm; m<=i__5;  ++ m)
					{
						i__6=ldata_.nmax;
						#pragma loop name term21_#1#0#1#0#0#0 
						for (nr=nnr; nr<=i__6;  ++ nr)
						{
							i__7=radial_.max__;
							#pragma loop name term21_#1#0#1#0#0#0#0 
							for (ij=1; ij<=i__7;  ++ ij)
							{
								v[ij-1]=((gg[(ij+((nr+(indr*100))*100))-10101]*gg[(ij+((m+(indm*100))*100))-10101])+(ff[(ij+((nr+(indr*100))*100))-10101]*ff[(ij+((m+(indm*100))*100))-10101]));
								/* L20: */
							}
							switch (yfun_(v, u,  & l,  & radial_.max__))
							{
								case 1:
								goto L901;
							}
							i__7=radial_.max__;
							#pragma loop name term21_#1#0#1#0#0#0#1 
							for (ij=1; ij<=i__7;  ++ ij)
							{
								uu[(ij+((nr+(m*100))*100))-10101]=u[ij-1];
								/* L19: */
							}
							/* L4: */
						}
						/* L3: */
					}
					i__5=((ldata_.lmax<<1)+1);
					#pragma loop name term21_#1#0#1#0#1 
					/* #pragma cetus reduction(+: uu1[((ij+((m+((k+(indn100))*100))*100))-1010101)], uu2[((ij+((m+((k+(indn*100))*100))*100))-1010101)])  */
					for (inds=1; inds<=i__5;  ++ inds)
					{
						indk1_( & inds,  & ks,  & kaps,  & ls,  & js,  & n0s);
						st_( & ks,  & nns);
						i__6=(lr+la);
						odd_( & i__6,  & i5);
						i__6=(ls+lb);
						odd_( & i__6,  & i6);
						if (((i5==0)&&(i6!=0))||((i5!=0)&&(i6==0)))
						{
							goto L155;
						}
						/* Computing MAX */
						((i__8=((i__6=(kapr-kapa)), abs(i__6))), (i__9=((i__7=(kaps-kapb)), abs(i__7))));
						k1min=max(i__8, i__9);
						/* Computing MIN */
						((i__6=((kapr+kapa)-1)), (i__7=((kaps+kapb)-1)));
						k1max=min(i__6, i__7);
						i__6=k1max;
						#pragma loop name term21_#1#0#1#0#1#0 
						/* #pragma cetus reduction(+: uu1[((ij+((m+((k+(indn100))*100))*100))-1010101)], uu2[((ij+((m+((k+(indn*100))*100))*100))-1010101)])  */
						for (k1=k1min; k1<=i__6;  ++ k1)
						{
							index1=irhoc_.ipxc[(i__+((j+((indr+((inds+(k1*100))*100))*100))*100))-101010101];
							i__7=ldata_.nmax;
							#pragma loop name term21_#1#0#1#0#1#0#0 
							for (nr=nnr; nr<=i__7;  ++ nr)
							{
								i__8=radial_.max__;
								#pragma loop name term21_#1#0#1#0#1#0#0#0 
								for (ij=1; ij<=i__8;  ++ ij)
								{
									v1[(ij+(nr*100))-101]=0.0;
									v2[(ij+(nr*100))-101]=0.0;
									/* L203: */
								}
								i__8=ldata_.nmax;
								#pragma loop name term21_#1#0#1#0#1#0#0#1 
								/* #pragma cetus reduction(+: v1[((ij+(nr100))-101)], v2[((ij+(nr*100))-101)])  */
								for (nss=nns; nss<=i__8;  ++ nss)
								{
									i__9=radial_.max__;
									#pragma loop name term21_#1#0#1#0#1#0#0#1#0 
									/* #pragma cetus reduction(+: v1[((ij+(nr100))-101)], v2[((ij+(nr*100))-101)])  */
									for (ij=1; ij<=i__9;  ++ ij)
									{
										v1[(ij+(nr*100))-101]+=(gg[(ij+((nss+(inds*100))*100))-10101]*rhocin_.xci[(nr+((nss+(index1*100))*100))-10101]);
										v2[(ij+(nr*100))-101]+=(ff[(ij+((nss+(inds*100))*100))-10101]*rhocin_.xci[(nr+((nss+(index1*100))*100))-10101]);
										/* L201: */
									}
									/* L41: */
								}
								/* L31: */
							}
							i__7=ldata_.nmax;
							#pragma loop name term21_#1#0#1#0#1#0#1 
							for (m=nnm; m<=i__7;  ++ m)
							{
								i__8=radial_.max__;
								#pragma loop name term21_#1#0#1#0#1#0#1#0 
								for (ij=1; ij<=i__8;  ++ ij)
								{
									uv1[(ij+(m*100))-101]=0.0;
									uv2[(ij+(m*100))-101]=0.0;
									/* L206: */
								}
								i__8=ldata_.nmax;
								#pragma loop name term21_#1#0#1#0#1#0#1#1 
								/* #pragma cetus reduction(+: uv1[((ij+(m100))-101)], uv2[((ij+(m*100))-101)])  */
								for (nr=nnr; nr<=i__8;  ++ nr)
								{
									i__9=radial_.max__;
									#pragma loop name term21_#1#0#1#0#1#0#1#1#0 
									/* #pragma cetus reduction(+: uv1[((ij+(m100))-101)], uv2[((ij+(m*100))-101)])  */
									for (ij=1; ij<=i__9;  ++ ij)
									{
										uv1[(ij+(m*100))-101]+=(v1[(ij+(nr*100))-101]*uu[(ij+((nr+(m*100))*100))-10101]);
										uv2[(ij+(m*100))-101]+=(v2[(ij+(nr*100))-101]*uu[(ij+((nr+(m*100))*100))-10101]);
										/* L202: */
									}
									/* L57: */
								}
								/* L51: */
							}
							i__7=((ldata_.lmax<<1)+1);
							#pragma loop name term21_#1#0#1#0#1#0#2 
							/* #pragma cetus reduction(+: uu1[((ij+((m+((k+(indn100))*100))*100))-1010101)], uu2[((ij+((m+((k+(indn*100))*100))*100))-1010101)])  */
							for (indn=1; indn<=i__7;  ++ indn)
							{
								indk1_( & indn,  & kn,  & kapn,  & ln,  & jn,  & n0);
								st_( & kn,  & nnn);
								i__8=(la+lm);
								odd_( & i__8,  & i5);
								i__8=(ln+lb);
								odd_( & i__8,  & i6);
								if (((i5==0)&&(i6!=0))||((i5!=0)&&(i6==0)))
								{
									goto L800;
								}
								i__9=((i__8=(kapn-kaps)), abs(i__8));
								i__10=((kapn+kaps)-1);
								trgi_( & i__9,  & i__10,  & l,  & i1);
								i__8=((ln+ls)+l);
								odd_( & i__8,  & i2);
								if ((i1*i2)==0)
								{
									goto L800;
								}
								c__=((pow_ii( & c_n1,  & l)*s_( & l,  & km,  & kr))*s_( & l,  & kn,  & ks));
								i__8=(((kapa+kapb)+kapn)+kapm);
								z1=((double)pow_ii( & c_n1,  & i__8));
								/* Computing MAX */
								((i__11=((i__8=(kapm-kapa)), abs(i__8))), (i__12=((i__9=(kapn-kapb)), abs(i__9))), (i__11=max(i__11, i__12)), (i__12=((i__10=(k1-l)), abs(i__10))));
								kmin=max(i__11, i__12);
								/* Computing MIN */
								((i__8=((kapm+kapa)-1)), (i__9=((kapn+kapb)-1)), (i__8=min(i__8, i__9)), (i__9=(k1+l)));
								kmax=min(i__8, i__9);
								i__8=kmax;
								#pragma loop name term21_#1#0#1#0#1#0#2#0 
								/* #pragma cetus reduction(+: uu1[((ij+((m+((k+(indn100))*100))*100))-1010101)], uu2[((ij+((m+((k+(indn*100))*100))*100))-1010101)])  */
								for (k=kmin; k<=i__8;  ++ k)
								{
									i__9=(k<<1);
									i__10=(k1<<1);
									i__11=(l<<1);
									c1=(((k<<1)+1.0)*d6j_( & jm,  & ja,  & i__9,  & i__10,  & i__11,  & jr));
									i__9=(k<<1);
									i__10=(k1<<1);
									i__11=(l<<1);
									cfin=(((c__*c1)*z1)*d6j_( & jn,  & jb,  & i__9,  & i__10,  & i__11,  & js));
									i__9=ldata_.nmax;
									#pragma loop name term21_#1#0#1#0#1#0#2#0#0 
									/* #pragma cetus reduction(+: uu1[((ij+((m+((k+(indn100))*100))*100))-1010101)], uu2[((ij+((m+((k+(indn*100))*100))*100))-1010101)])  */
									for (m=nnm; m<=i__9;  ++ m)
									{
										i__10=radial_.max__;
										#pragma loop name term21_#1#0#1#0#1#0#2#0#0#0 
										/* #pragma cetus reduction(+: uu1[((ij+((m+((k+(indn100))*100))*100))-1010101)], uu2[((ij+((m+((k+(indn*100))*100))*100))-1010101)])  */
										for (ij=1; ij<=i__10;  ++ ij)
										{
											uu1[(ij+((m+((k+(indn*100))*100))*100))-1010101]+=(uv1[(ij+(m*100))-101]*cfin);
											uu2[(ij+((m+((k+(indn*100))*100))*100))-1010101]+=(uv2[(ij+(m*100))-101]*cfin);
											/* L280: */
										}
										/* L59: */
									}
									/* L219: */
								}
								/* L188: */
								L800:
								;
							}
							/* L119: */
						}
						/* L7: */
						L155:
						;
					}
					/* L500: */
					L900:
					;
				}
				/* L2: */
			}
			i__3=((ldata_.lmax<<1)+1);
			#pragma loop name term21_#1#0#2 
			for (indn=1; indn<=i__3;  ++ indn)
			{
				indk1_( & indn,  & kn,  & kapn,  & ln,  & jn,  & n0);
				st_( & kn,  & nnn);
				i__4=(la+lm);
				odd_( & i__4,  & i5);
				i__4=(ln+lb);
				odd_( & i__4,  & i6);
				if (((i5==0)&&(i6!=0))||((i5!=0)&&(i6==0)))
				{
					goto L810;
				}
				/* Computing MAX */
				((i__6=((i__4=(kapm-kapa)), abs(i__4))), (i__7=((i__5=(kapn-kapb)), abs(i__5))));
				kmin=max(i__6, i__7);
				/* Computing MIN */
				((i__4=((kapm+kapa)-1)), (i__5=((kapn+kapb)-1)));
				kmax=min(i__4, i__5);
				i__4=kmax;
				#pragma loop name term21_#1#0#2#0 
				for (k=kmin; k<=i__4;  ++ k)
				{
					index=irhoc_.ipxc[(i__+((j+((indm+((indn+(k*100))*100))*100))*100))-101010101];
					i__5=ldata_.nmax;
					#pragma loop name term21_#1#0#2#0#0 
					for (n=nnn; n<=i__5;  ++ n)
					{
						i__6=ldata_.nmax;
						#pragma loop name term21_#1#0#2#0#0#0 
						for (m=nnm; m<=i__6;  ++ m)
						{
							i__7=radial_.max__;
							#pragma loop name term21_#1#0#2#0#0#0#0 
							for (ij=1; ij<=i__7;  ++ ij)
							{
								vv[ij-1]=(((uu1[(ij+((m+((k+(indn*100))*100))*100))-1010101]*gg[(ij+((n+(indn*100))*100))-10101])+(uu2[(ij+((m+((k+(indn*100))*100))*100))-1010101]*ff[(ij+((n+(indn*100))*100))-10101]))*rp[ij-1]);
								/* L205: */
							}
							rhocout_.xco[(m+((n+(index*100))*100))-10101]+=rint_(vv,  & c__1,  & radial_.max__,  & c__11,  & radial_.h__);
							/* L53: */
						}
						/* L52: */
					}
					/* L319: */
				}
				/* L189: */
				L810:
				;
			}
			/* L1: */
		}
		/* L5: */
	}
	L901:
	_ret_val_0=0;
	return _ret_val_0;
	return _ret_val_0;
}
