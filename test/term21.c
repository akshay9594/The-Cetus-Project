/* term21.f -- translated by f2c (version 20160102).
   You must link the resulting object file with libf2c:
	on Microsoft Windows system, link with libf2c.lib;
	on Linux or Unix systems, link with .../path/to/libf2c.a -lm
	or, if you install libf2c.a in a standard place, with -lf2c -lm
	-- in that order, at the end of the command line, as in
		cc *.o -lf2c -lm
	Source for libf2c is in /netlib/f2c/libf2c.zip, e.g.,

		http://www.netlib.org/f2c/libf2c.zip
*/

//#include "f2c.h"

/* Common Block Declarations */

struct {
    double h__;
    int max__;
} radial_;

#define radial_1 radial_

struct {
    int nmax, lmax, nmax1, lmax1;
} ldata_;

#define ldata_1 ldata_

struct {
    double eo[100];
    int no[100], ko[100], ncore;
} core_;

#define core_1 core_

struct {
    int ipxc[10000000000]	/* was [100][100][100][100][100] */, icchan;
} irhoc_;

#define irhoc_1 irhoc_

struct {
    double xci[1000000]	/* was [100][100][100] */, rmi[10000]	/* 
	    was [100][100] */;
} rhocin_;

#define rhocin_1 rhocin_

struct {
    double xco[1000000]	/* was [100][100][100] */, rmo[10000]	/* 
	    was [100][100] */;
} rhocout_;

#define rhocout_1 rhocout_

struct {
    int iqa[100], iqb[100];
} iqqq_;

#define iqqq_1 iqqq_

/* Table of constant values */


static int c_n1 = -1;
static int c__1 = 1;
static int c__11 = 11;

/* Subroutine */ int term21_(void)
{
    /* System generated locals */
    int i__1, i__2, i__3, i__4, i__5, i__6, i__7, i__8, i__9, i__10, 
	    i__11, i__12;

    /* Builtin functions */
    int pow_ii(int *, int *);

    /* Local variables */
    static double c__;
    static int i__, j, k, l, m, n;
    extern double s_(int *, int *, int *);
    static double u[100], v[100], c1;
    static int i1, i2, k1, m0, i5, i6, n0;
    static double v1[10000]	/* was [100][100] */, v2[10000]	/* was [100][
	    100] */, z1;
    static int ja;
    static double ff[1000000]	/* was [100][100][100] */;
    static int ka;
    static double gg[1000000]	/* was [100][100][100] */;
    static int na, la, kb, nb, lb, jb, km, lm, jm, ij, kr, lr, jr, nr;
    static double rp[100];
    static int ks, ls, js, kn;
    extern /* Subroutine */ int st_(int *, int *);
    static int ln, jn;
    static double uu[1000000]	/* was [100][100][100] */, vv[100];
    static int n0a, n0b;
    extern double d6j_(int *, int *, int *, int *, 
	    int *, int *);
    static int kk1, n0r, n0s;
    static double uu1[100000000]	/* was [100][100][100][100] */, uv1[
	    10000]	/* was [100][100] */, uv2[10000]	/* was [100][
	    100] */, uu2[100000000]	/* was [100][100][100][100] */;
    extern /* Subroutine */ int odd_(int *, int *), klj_(int *, 
	    int *, int *, int *, int *, int *);
    static int iiq, nnm, nnn, nnr, nns, nss, nxx, nhf1, inda, kapa, kapb, 
	    indb;
    static double cfin;
    static int indm, kapm, indn, kapn, indr, kapr, inds, kaps, kmin, kmax;
    extern /* Subroutine */ int trgi_(int *, int *, int *, 
	    int *);
    static int nnmq;
    extern double rint_(double *, int *, int *, int *, 
	    double *);
    extern /* Subroutine */ int yfun_(double *, double *, int *, 
	    int *);
    static int ittt;
    extern /* Subroutine */ int indk1_(int *, int *, int *, 
	    int *, int *, int *);
    static int k1min, l1min, k1max, l1max, index, index1;

/*      include "all.par" */
    iiq = 0;
    i__1 = core_1.ncore;
    for (i1 = 1; i1 <= i__1; ++i1) {
	i__2 = core_1.ncore;
	for (i2 = 1; i2 <= i__2; ++i2) {
	    if (i1 >= i2) {
		++iiq;
		iqqq_1.iqa[iiq - 1] = i1;
		iqqq_1.iqb[iiq - 1] = i2;
/*          write (*,'(3i5)') i1,i2,iiq,nnh */
	    }
/* L110: */
	}
/* L109: */
    }
    nnmq = iiq;
    i__1 = nnmq;
    for (ittt = 1; ittt <= i__1; ++ittt) {
	i__ = iqqq_1.iqa[ittt - 1];
	j = iqqq_1.iqb[ittt - 1];
	ka = core_1.ko[i__ - 1];
	na = core_1.no[i__ - 1];
	klj_(&ka, &kapa, &la, &ja, &inda, &n0a);
	kb = core_1.ko[j - 1];
	nb = core_1.no[j - 1];
	klj_(&kb, &kapb, &lb, &jb, &indb, &n0b);
	i__2 = (ldata_1.lmax << 1) + 1;
	for (indm = 1; indm <= i__2; ++indm) {
	    indk1_(&indm, &km, &kapm, &lm, &jm, &m0);
	    st_(&km, &nnm);
	    i__3 = kk1;
	    for (indn = 1; indn <= i__3; ++indn) {
		i__4 = kk1;
		for (k = 0; k <= i__4; ++k) {
		    i__5 = nxx;
		    for (m = 1; m <= i__5; ++m) {
			i__6 = nhf1;
			for (ij = 1; ij <= i__6; ++ij) {
			    uu1[ij + (m + (k + indn * 100) * 100) * 100 - 
				    1010101] = 0.;
			    uu2[ij + (m + (k + indn * 100) * 100) * 100 - 
				    1010101] = 0.;
/* L94: */
			}
/* L93: */
		    }
/* L92: */
		}
/* L91: */
	    }
	    i__3 = (ldata_1.lmax << 1) + 1;
	    for (indr = 1; indr <= i__3; ++indr) {
		indk1_(&indr, &kr, &kapr, &lr, &jr, &n0r);
		st_(&kr, &nnr);
		l1min = (i__4 = kapm - kapr, abs(i__4));
		l1max = kapm + kapr - 1;
		i__4 = l1max;
		for (l = l1min; l <= i__4; ++l) {
		    i__5 = lm + lr + l;
		    odd_(&i__5, &i5);
		    if (i5 == 0) {
			goto L900;
		    }
		    i__5 = ldata_1.nmax;
		    for (m = nnm; m <= i__5; ++m) {
			i__6 = ldata_1.nmax;
			for (nr = nnr; nr <= i__6; ++nr) {
			    i__7 = radial_1.max__;
			    for (ij = 1; ij <= i__7; ++ij) {
				v[ij - 1] = gg[ij + (nr + indr * 100) * 100 - 
					10101] * gg[ij + (m + indm * 100) * 
					100 - 10101] + ff[ij + (nr + indr * 
					100) * 100 - 10101] * ff[ij + (m + 
					indm * 100) * 100 - 10101];
/* L20: */
			    }
			    switch (yfun_(v, u, &l, &radial_1.max__)) {
				case 1:  goto L901;
			    }
			    i__7 = radial_1.max__;
			    for (ij = 1; ij <= i__7; ++ij) {
				uu[ij + (nr + m * 100) * 100 - 10101] = u[ij 
					- 1];
/* L19: */
			    }
/* L4: */
			}
/* L3: */
		    }
		    i__5 = (ldata_1.lmax << 1) + 1;
		    for (inds = 1; inds <= i__5; ++inds) {
			indk1_(&inds, &ks, &kaps, &ls, &js, &n0s);
			st_(&ks, &nns);
			i__6 = lr + la;
			odd_(&i__6, &i5);
			i__6 = ls + lb;
			odd_(&i__6, &i6);
			if (i5 == 0 && i6 != 0 || i5 != 0 && i6 == 0) {
			    goto L155;
			}
/* Computing MAX */
			i__8 = (i__6 = kapr - kapa, abs(i__6)), i__9 = (i__7 =
				 kaps - kapb, abs(i__7));
			k1min = max(i__8,i__9);
/* Computing MIN */
			i__6 = kapr + kapa - 1, i__7 = kaps + kapb - 1;
			k1max = min(i__6,i__7);
			i__6 = k1max;
			for (k1 = k1min; k1 <= i__6; ++k1) {
			    index1 = irhoc_1.ipxc[i__ + (j + (indr + (inds + 
				    k1 * 100) * 100) * 100) * 100 - 101010101]
				    ;
			    i__7 = ldata_1.nmax;
			    for (nr = nnr; nr <= i__7; ++nr) {
				i__8 = radial_1.max__;
				for (ij = 1; ij <= i__8; ++ij) {
				    v1[ij + nr * 100 - 101] = 0.;
				    v2[ij + nr * 100 - 101] = 0.;
/* L203: */
				}
				i__8 = ldata_1.nmax;
				for (nss = nns; nss <= i__8; ++nss) {
				    i__9 = radial_1.max__;
				    for (ij = 1; ij <= i__9; ++ij) {
					v1[ij + nr * 100 - 101] += gg[ij + (
						nss + inds * 100) * 100 - 
						10101] * rhocin_1.xci[nr + (
						nss + index1 * 100) * 100 - 
						10101];
					v2[ij + nr * 100 - 101] += ff[ij + (
						nss + inds * 100) * 100 - 
						10101] * rhocin_1.xci[nr + (
						nss + index1 * 100) * 100 - 
						10101];
/* L201: */
				    }
/* L41: */
				}
/* L31: */
			    }
			    i__7 = ldata_1.nmax;
			    for (m = nnm; m <= i__7; ++m) {
				i__8 = radial_1.max__;
				for (ij = 1; ij <= i__8; ++ij) {
				    uv1[ij + m * 100 - 101] = 0.;
				    uv2[ij + m * 100 - 101] = 0.;
/* L206: */
				}
				i__8 = ldata_1.nmax;
				for (nr = nnr; nr <= i__8; ++nr) {
				    i__9 = radial_1.max__;
				    for (ij = 1; ij <= i__9; ++ij) {
					uv1[ij + m * 100 - 101] += v1[ij + nr 
						* 100 - 101] * uu[ij + (nr + 
						m * 100) * 100 - 10101];
					uv2[ij + m * 100 - 101] += v2[ij + nr 
						* 100 - 101] * uu[ij + (nr + 
						m * 100) * 100 - 10101];
/* L202: */
				    }
/* L57: */
				}
/* L51: */
			    }
			    i__7 = (ldata_1.lmax << 1) + 1;
			    for (indn = 1; indn <= i__7; ++indn) {
				indk1_(&indn, &kn, &kapn, &ln, &jn, &n0);
				st_(&kn, &nnn);
				i__8 = la + lm;
				odd_(&i__8, &i5);
				i__8 = ln + lb;
				odd_(&i__8, &i6);
				if (i5 == 0 && i6 != 0 || i5 != 0 && i6 == 0) 
					{
				    goto L800;
				}
				i__9 = (i__8 = kapn - kaps, abs(i__8));
				i__10 = kapn + kaps - 1;
				trgi_(&i__9, &i__10, &l, &i1);
				i__8 = ln + ls + l;
				odd_(&i__8, &i2);
				if (i1 * i2 == 0) {
				    goto L800;
				}
				c__ = pow_ii(&c_n1, &l) * s_(&l, &km, &kr) * 
					s_(&l, &kn, &ks);
				i__8 = kapa + kapb + kapn + kapm;
				z1 = (double) pow_ii(&c_n1, &i__8);
/* Computing MAX */
				i__11 = (i__8 = kapm - kapa, abs(i__8)), 
					i__12 = (i__9 = kapn - kapb, abs(i__9)
					), i__11 = max(i__11,i__12), i__12 = (
					i__10 = k1 - l, abs(i__10));
				kmin = max(i__11,i__12);
/* Computing MIN */
				i__8 = kapm + kapa - 1, i__9 = kapn + kapb - 
					1, i__8 = min(i__8,i__9), i__9 = k1 + 
					l;
				kmax = min(i__8,i__9);
				i__8 = kmax;
				for (k = kmin; k <= i__8; ++k) {
				    i__9 = k << 1;
				    i__10 = k1 << 1;
				    i__11 = l << 1;
				    c1 = ((k << 1) + 1.) * d6j_(&jm, &ja, &
					    i__9, &i__10, &i__11, &jr);
				    i__9 = k << 1;
				    i__10 = k1 << 1;
				    i__11 = l << 1;
				    cfin = c__ * c1 * z1 * d6j_(&jn, &jb, &
					    i__9, &i__10, &i__11, &js);
				    i__9 = ldata_1.nmax;
				    for (m = nnm; m <= i__9; ++m) {
					i__10 = radial_1.max__;
					for (ij = 1; ij <= i__10; ++ij) {
					    uu1[ij + (m + (k + indn * 100) * 
						    100) * 100 - 1010101] += 
						    uv1[ij + m * 100 - 101] * 
						    cfin;
					    uu2[ij + (m + (k + indn * 100) * 
						    100) * 100 - 1010101] += 
						    uv2[ij + m * 100 - 101] * 
						    cfin;
/* L280: */
					}
/* L59: */
				    }
/* L219: */
				}
L800:
/* L188: */
				;
			    }
/* L119: */
			}
L155:
/* L7: */
			;
		    }
L900:
/* L500: */
		    ;
		}
/* L2: */
	    }
	    i__3 = (ldata_1.lmax << 1) + 1;
	    for (indn = 1; indn <= i__3; ++indn) {
		indk1_(&indn, &kn, &kapn, &ln, &jn, &n0);
		st_(&kn, &nnn);
		i__4 = la + lm;
		odd_(&i__4, &i5);
		i__4 = ln + lb;
		odd_(&i__4, &i6);
		if (i5 == 0 && i6 != 0 || i5 != 0 && i6 == 0) {
		    goto L810;
		}
/* Computing MAX */
		i__6 = (i__4 = kapm - kapa, abs(i__4)), i__7 = (i__5 = kapn - 
			kapb, abs(i__5));
		kmin = max(i__6,i__7);
/* Computing MIN */
		i__4 = kapm + kapa - 1, i__5 = kapn + kapb - 1;
		kmax = min(i__4,i__5);
		i__4 = kmax;
		for (k = kmin; k <= i__4; ++k) {
		    index = irhoc_1.ipxc[i__ + (j + (indm + (indn + k * 100) *
			     100) * 100) * 100 - 101010101];
		    i__5 = ldata_1.nmax;
		    for (n = nnn; n <= i__5; ++n) {
			i__6 = ldata_1.nmax;
			for (m = nnm; m <= i__6; ++m) {
			    i__7 = radial_1.max__;
			    for (ij = 1; ij <= i__7; ++ij) {
				vv[ij - 1] = (uu1[ij + (m + (k + indn * 100) *
					 100) * 100 - 1010101] * gg[ij + (n + 
					indn * 100) * 100 - 10101] + uu2[ij + 
					(m + (k + indn * 100) * 100) * 100 - 
					1010101] * ff[ij + (n + indn * 100) * 
					100 - 10101]) * rp[ij - 1];
/* L205: */
			    }
			    rhocout_1.xco[m + (n + index * 100) * 100 - 10101]
				     += rint_(vv, &c__1, &radial_1.max__, &
				    c__11, &radial_1.h__);
/* L53: */
			}
/* L52: */
		    }
/* L319: */
		}
L810:
/* L189: */
		;
	    }
/* L1: */
	}
/* L5: */
    }
L901:
    return 0;
} /* term21_ */

