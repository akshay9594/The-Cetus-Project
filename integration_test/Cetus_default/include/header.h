//-------------------------------------------------------------------------//
//                                                                         //
//  This benchmark is an OpenMP C version of the NPB UA code. This OpenMP  //
//  C version is developed by the Center for Manycore Programming at Seoul //
//  National University and derived from the OpenMP Fortran versions in    //
//  "NPB3.3-OMP" developed by NAS.                                         //
//                                                                         //
//  Permission to use, copy, distribute and modify this software for any   //
//  purpose with or without fee is hereby granted. This software is        //
//  provided "as is" without express or implied warranty.                  //
//                                                                         //
//  Information on NPB 3.3, including the technical report, the original   //
//  specifications, source code, results and information on how to submit  //
//  new results, is available at:                                          //
//                                                                         //
//           http://www.nas.nasa.gov/Software/NPB/                         //
//                                                                         //
//  Send comments or suggestions for this OpenMP C version to              //
//  cmp@aces.snu.ac.kr                                                     //
//                                                                         //
//          Center for Manycore Programming                                //
//          School of Computer Science and Engineering                     //
//          Seoul National University                                      //
//          Seoul 151-744, Korea                                           //
//                                                                         //
//          E-mail:  cmp@aces.snu.ac.kr                                    //
//                                                                         //
//-------------------------------------------------------------------------//

//-------------------------------------------------------------------------//
// Authors: Sangmin Seo, Jungwon Kim, Jun Lee, Jeongho Nah, Gangwon Jo,    //
//          and Jaejin Lee                                                 //
//-------------------------------------------------------------------------//


#define LELT           8800

#define LMOR           334600

// Array dimensions     
#define LX1       5
#define LNJE      2
#define NSIDES    6
#define NXYZ      (LX1*LX1*LX1)

/* common /usrdati/ */
extern int fre, niter, nmxh;

/* common /usrdatr/ */
extern double alpha, dlmin, dtime;

/* common /dimn/ */
extern int nelt, ntot, nmor, nvertex;

/* common /bench1/ */
extern double x0, _y0, z0, time;

#define VELX    3.0
#define VELY    3.0
#define VELZ    3.0
#define VISC    0.005
#define X00     (3.0/7.0)
#define Y00     (2.0/7.0)
#define Z00     (2.0/7.0)

// double arrays associated with collocation points
/* common /colldp/ */
extern double ta1   [LELT][LX1][LX1][LX1];
extern double ta2   [LELT][LX1][LX1][LX1];
extern double trhs  [LELT][LX1][LX1][LX1];
extern double t     [LELT][LX1][LX1][LX1];
extern double tmult [LELT][LX1][LX1][LX1];
extern double dpcelm[LELT][LX1][LX1][LX1];
extern double pdiff [LELT][LX1][LX1][LX1];
extern double pdiffp[LELT][LX1][LX1][LX1];

// double arrays associated with mortar points
/* common /mortdp/ */
extern double umor   [LMOR];
extern double mormult[LMOR];
extern double tmort  [LMOR];
extern double tmmor  [LMOR];
extern double rmor   [LMOR];
extern double dpcmor [LMOR];
extern double pmorx  [LMOR];
extern double ppmor  [LMOR];

// integer arrays associated with element faces
/* common/facein/ */
extern int idmo    [LELT][NSIDES][LNJE][LNJE][LX1][LX1]; 
extern int idel    [LELT][NSIDES][LX1][LX1]; 
extern int sje     [LELT][NSIDES][2][2]; 
extern int sje_new [LELT][NSIDES][2][2];
extern int ijel    [LELT][NSIDES][2]; 
extern int ijel_new[LELT][NSIDES][2];
extern int cbc     [LELT][NSIDES]; /**/
extern int cbc_new [LELT][NSIDES]; /**/

// integer array associated with vertices
/* common /vin/ */
extern int vassign[LELT][8];
extern int emo    [8*LELT][8][2];
extern int nemo   [8*LELT];

// integer array associated with element edges
/* common /edgein/ */
extern int diagn[LELT][12][2];

// integer arrays associated with elements
/* common /eltin/ */
extern int tree        [LELT];
extern int treenew     [LELT];
extern int mt_to_id    [LELT];
extern int mt_to_id_old[LELT];
extern int id_to_mt    [LELT];
extern int newc        [LELT]; /**/
extern int newi        [LELT]; /**/
extern int newe        [LELT]; /**/
extern int ref_front_id[LELT]; /**/
extern int ich         [LELT]; /**/
extern int size_e      [LELT];
extern int front       [LELT];
extern int action      [LELT];


// small arrays
/* common /transr/ */
extern double qbnew[2][LX1][LX1-2];
extern double bqnew[2][LX1-2][LX1-2];


// gauss-labotto and gauss points
/* common /gauss/ */
extern double zgm1[LX1];

// weights
/* common /wxyz/ */
extern double wxm1[LX1];
extern double w3m1[LX1][LX1][LX1];

// coordinate of element vertices
/* common /coord/ */
extern double xc[LELT][8];
extern double yc[LELT][8];
extern double zc[LELT][8];
extern double xc_new[LELT][8];
extern double yc_new[LELT][8];
extern double zc_new[LELT][8];

// dertivative matrices d/dr
/* common /dxyz/ */
extern double dxm1[LX1][LX1];
extern double dxtm1[LX1][LX1];
extern double wdtdr[LX1][LX1];

// interpolation operators
/* common /ixyz/ */
extern double ixm31 [LX1*2-1][LX1];
extern double ixtm31[LX1][LX1*2-1];
extern double ixmc1 [LX1][LX1];
extern double ixtmc1[LX1][LX1];
extern double ixmc2 [LX1][LX1];
extern double ixtmc2[LX1][LX1];
extern double map2  [LX1];
extern double map4  [LX1];

// collocation location within an element
/* common /xfracs/ */
extern double xfrac[LX1];
      
// We store some tables of useful topological constants
// These constants are intialized in a block data 'top_constants'
/* common /top_consts/ */
extern int f_e_ef[6][4];
extern int e_c[8][3];
extern int local_corner[6][8];
extern int cal_nnb[8][3];
extern int oplc[4];
extern int cal_iijj[4][2];
extern int cal_intempx[6][4];
extern int c_f[6][4];
extern int le_arr[3][2][4];
extern int jjface[6];
extern int e_face2[6][4];
extern int op[4];
extern int localedgenumber[12][6];
extern int edgenumber[6][4];
extern int f_c[8][3];
extern int e1v1[6][6];
extern int e2v1[6][6];
extern int e1v2[6][6];
extern int e2v2[6][6];
extern int children[6][4];
extern int iijj[4][2];
extern int v_end[2];
extern int face_l1[3];
extern int face_l2[3];
extern int face_ld[3];

// Timer parameters
/* common /timing/ */
#define t_total       1
#define t_init        2
#define t_convect     3
#define t_transfb_c   4
#define t_diffusion   5
#define t_transf      6
#define t_transfb     7
#define t_adaptation  8
#define t_transf2     9
#define t_add2        10
#define t_last        10

// Locks used for atomic updates
/* common /sync_cmn/ */

static int r_init(double a[], int n, double _const);
