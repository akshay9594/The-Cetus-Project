
/*  
  Example loop from the UA benchmark in the NAS
  Parallel Benchmarks (NPB 3.3)
*/

  #include <stdio.h>
  #include <math.h>
  #include <stdlib.h>
  #include "./include/header.h"

int main(){

  double tmor[LELT], tx[LELT],tmp[LX1][LX1],u[LX1][LX1][LX1],r[LX1][LX1][LX1];
  int ig1, ig2, ig3, ig4, ie, iface, il1, il2, il3, il4, ntemp;
  int nnje, ije1, ije2, col, i, j, ig, il;
  int k,iz;
  const double third = 1.0/3.0;


  r_init(tmort, nmor, 0.0);
  r_init(mormult, nmor, 0.0);

  for (ie = 0; ie < nelt; ie++) {
    for (iface = 0; iface < NSIDES; iface++) {

      if (cbc[ie][iface] != 3) {
        il1 = idel[ie][iface][0][0];
        il2 = idel[ie][iface][0][LX1-1];
        il3 = idel[ie][iface][LX1-1][0];
        il4 = idel[ie][iface][LX1-1][LX1-1];
        ig1 = idmo[ie][iface][0][0][0][0];
        ig2 = idmo[ie][iface][1][0][0][LX1-1];
        ig3 = idmo[ie][iface][0][1][LX1-1][0];
        ig4 = idmo[ie][iface][1][1][LX1-1][LX1-1];

        tmort[ig1] = tmort[ig1]+tx[il1]*third;
        tmort[ig2] = tmort[ig2]+tx[il2]*third;
        tmort[ig3] = tmort[ig3]+tx[il3]*third;
        tmort[ig4] = tmort[ig4]+tx[il4]*third;
        mormult[ig1] = mormult[ig1]+third;
        mormult[ig2] = mormult[ig2]+third;
        mormult[ig3] = mormult[ig3]+third;
        mormult[ig4] = mormult[ig4]+third;

        for (col = 1; col < LX1-1; col++) {
          for (j = 1; j < LX1-1; j++) {
            il = idel[ie][iface][col][j];
            ig = idmo[ie][iface][0][0][col][j];
            tmort[ig] = tmort[ig]+tx[il];
            mormult[ig] = mormult[ig]+1.0;
          }
        }

        if (idmo[ie][iface][0][0][0][LX1-1] == -1) {
          for (j = 1; j < LX1-1; j++) {
            il = idel[ie][iface][0][j];
            ig = idmo[ie][iface][0][0][0][j];
            tmort[ig] = tmort[ig]+tx[il]*0.5;
            mormult[ig] = mormult[ig]+0.5;
          }
        }

        if (idmo[ie][iface][1][0][1][LX1-1] == -1) {
          for (j = 1; j < LX1-1; j++) {
            il = idel[ie][iface][j][LX1-1];
            ig = idmo[ie][iface][0][0][j][LX1-1];
            tmort[ig] = tmort[ig]+tx[il]*0.5;
            mormult[ig] = mormult[ig]+0.5;
          }
        }

        if (idmo[ie][iface][0][1][LX1-1][1] == -1) {
          for (j = 1; j < LX1-1; j++) {
            il = idel[ie][iface][LX1-1][j];
            ig = idmo[ie][iface][0][0][LX1-1][j];
            tmort[ig] = tmort[ig]+tx[il]*0.5;
            mormult[ig] = mormult[ig]+0.5;
          }
        }

        if (idmo[ie][iface][0][0][LX1-1][0] == -1) {
          for (j = 1; j < LX1-1; j++) {
            il = idel[ie][iface][j][0];
            ig = idmo[ie][iface][0][0][j][0];
            tmort[ig] = tmort[ig]+tx[il]*0.5;
            mormult[ig] = mormult[ig]+0.5;
          }
        }
      }
    }
  }

   return 0;


 }

 

static int r_init(double a[], int n, double _const)
{
  int i;

  for (i = 0; i < n; i++) {
    a[i] = _const;
  }
  return 0;
}
