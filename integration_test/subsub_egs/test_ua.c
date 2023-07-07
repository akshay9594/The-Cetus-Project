
/*  
  Example loop from the UA benchmark in the NAS
  Parallel Benchmarks (NPB 3.3)
*/

  #include <stdio.h>
  #include <math.h>
  #include <stdlib.h>
  #include "./include/header.h"

int main(){

  double tmp[2][LX1][LX1];
  double tmor[LELT], tx[LELT];
  int ig1, ig2, ig3, ig4, ie, iface, il1, il2, il3, il4, ntemp;
  int nnje, ije1, ije2, col, i, j, ig, il;
  int k;
  v_end[0] = 0;
  v_end[1] = LX1-1;

   
     for (k = 0; k < LELT; k++) {
    ntemp = k*LX1*LX1*LX1;
    for (j = 0; j < LX1; j++) {
      for (i = 0; i < LX1; i++) {
        idel[k][0][j][i] = ntemp+i*LX1 + j*LX1*LX1+LX1 - 1;
        idel[k][1][j][i] = ntemp+i*LX1 + j*LX1*LX1;
        idel[k][2][j][i] = ntemp+i*1 + j*LX1*LX1+LX1*(LX1-1);
        idel[k][3][j][i] = ntemp+i*1 + j*LX1*LX1;
        idel[k][4][j][i] = ntemp+i*1 + j*LX1+LX1*LX1*(LX1-1);
        idel[k][5][j][i] = ntemp+i*1 + j*LX1;
      }
    }
  }
 
  for (ie = 0; ie < nelt; ie++) {
    for (iface = 0; iface < NSIDES; iface++) {
      // get the collocation point index of the four local corners on the
      // face iface of element ie
      il1 = idel[ie][iface][0][0];
      il2 = idel[ie][iface][0][LX1-1];
      il3 = idel[ie][iface][LX1-1][0];
      il4 = idel[ie][iface][LX1-1][LX1-1];
       
      // get the mortar indices of the four local corners
      ig1 = idmo[ie][iface][0][0][0][0];
      ig2 = idmo[ie][iface][1][0][0][LX1-1];
      ig3 = idmo[ie][iface][0][1][LX1-1][0];
      ig4 = idmo[ie][iface][1][1][LX1-1][LX1-1];

      // copy the value from tmor to tx for these four local corners
      tx[il1] = tmor[ig1];
      tx[il2] = tmor[ig2];
      tx[il3] = tmor[ig3];
      tx[il4] = tmor[ig4];

      // nnje=1 for conforming faces, nnje=2 for nonconforming faces
      if (cbc[ie][iface] == 3) {
        nnje = 2;
      } else {
        nnje = 1;
      }

      // for nonconforming faces
      if (nnje == 2) {
        // nonconforming faces have four pieces of mortar, first map them to
        // two intermediate mortars, stored in tmp
        //r_init((double *)tmp, LX1*LX1*2, 0.0);

        for(int x=0; x<nnje; x++){
          for(int y=0; y< LX1; y++){
            for(int z =0; z<LX1; z++){
              tmp[x][y][z] = 0.0;
            }
          }
        }

        for (ije1 = 0; ije1 < nnje; ije1++) {
          for (ije2 = 0; ije2 < nnje; ije2++) {
            for (col = 0; col < LX1; col++) {
              // in each row col, when coloumn i=1 or LX1, the value
              // in tmor is copied to tmp
              i = v_end[ije2];
              ig = idmo[ie][iface][ije2][ije1][col][i];
              tmp[ije1][col][i] = tmor[ig];

              // in each row col, value in the interior three collocation
              // points is computed by apply mapping matrix qbnew to tmor
              for (i = 1; i < LX1-1; i++) {
                il = idel[ie][iface][col][i];
                for (j = 0; j < LX1; j++) {
                  ig = idmo[ie][iface][ije2][ije1][col][j];
                  tmp[ije1][col][i] = tmp[ije1][col][i] +
                    qbnew[ije2][j][i-1]*tmor[ig];
                }
              }
            }
          }
        }

        // mapping from two pieces of intermediate mortar tmp to element
        // face tx
        for (ije1 = 0; ije1 < nnje; ije1++) {
          // the first column, col=0, is an edge of face iface.
          // the value on the three interior collocation points, tx, is
          // computed by applying mapping matrices qbnew to tmp.
          // the mapping result is divided by 2, because there will be
          // duplicated contribution from another face sharing this edge.
          col = 0;
          for (i = 1; i < LX1-1; i++) {
            il= idel[ie][iface][i][col];
            for (j = 0; j < LX1; j++) {
              tx[il] = tx[il] + qbnew[ije1][j][i-1]*
                tmp[ije1][j][col]*0.5;
            }
          }

          // for column 1 ~ lx-2
          for (col = 1; col < LX1-1; col++) {
            //when i=0 or LX1-1, the collocation points are also on an edge of
            // the face, so the mapping result also needs to be divided by 2
            i = v_end[ije1];
            il = idel[ie][iface][i][col];
            tx[il] = tx[il]+tmp[ije1][i][col]*0.5;

            // compute the value at interior collocation points in
            // columns 1 ~ LX1-1
            for (i = 1; i < LX1-1; i++) {
              il = idel[ie][iface][i][col];
              for (j = 0; j < LX1; j++) {
                tx[il] = tx[il] + qbnew[ije1][j][i-1]* tmp[ije1][j][col];
              }
            }
          }

          // same as col=0
          col = LX1-1;
          for (i = 1; i < LX1-1; i++) {
            il = idel[ie][iface][i][col];
            for (j = 0; j < LX1; j++) {
              tx[il] = tx[il] + qbnew[ije1][j][i-1]*
                tmp[ije1][j][col]*0.5;
            }
          }
        }

        // for conforming faces
       } 
      else {
        // face interior
        for (col = 1; col < LX1-1; col++) {
          for (i = 1; i < LX1-1; i++) {
            il = idel[ie][iface][col][i];
            ig = idmo[ie][iface][0][0][col][i];
            tx[il] = tmor[ig];
          }
        }

        // edges of conforming faces

        // if local edge 0 is a nonconforming edge
        if (idmo[ie][iface][0][0][0][LX1-1] != -1) {
          for (i = 1; i < LX1-1; i++) {
            il = idel[ie][iface][0][i];
            for (ije1 = 0; ije1 < 2; ije1++) {
              for (j = 0; j < LX1; j++) {
                ig = idmo[ie][iface][ije1][0][0][j];
                tx[il] = tx[il] + qbnew[ije1][j][i-1]*tmor[ig]*0.5;
              }
            }
          }

          // if local edge 0 is a conforming edge
        } else {
          for (i = 1; i < LX1-1; i++) {
            il = idel[ie][iface][0][i];
            ig = idmo[ie][iface][0][0][0][i];
            tx[il] = tmor[ig];
          }
        }

        // if local edge 1 is a nonconforming edge
        if (idmo[ie][iface][1][0][1][LX1-1] != -1) {
          for (i = 1; i < LX1-1; i++) {
            il = idel[ie][iface][i][LX1-1];
            for (ije1 = 0; ije1 < 2; ije1++) {
              for (j = 0; j < LX1; j++) {
                ig = idmo[ie][iface][1][ije1][j][LX1-1];
                tx[il] = tx[il] + qbnew[ije1][j][i-1]*tmor[ig]*0.5;
              }
            }
          }

          // if local edge 1 is a conforming edge
        } else {
          for (i = 1; i < LX1-1; i++) {
            il = idel[ie][iface][i][LX1-1];
            ig = idmo[ie][iface][0][0][i][LX1-1];
            tx[il] = tmor[ig];
          }
        }

        // if local edge 2 is a nonconforming edge
        if (idmo[ie][iface][0][1][LX1-1][1] != -1) {
          for (i = 1; i < LX1-1; i++) {
            il = idel[ie][iface][LX1-1][i];
            for (ije1 = 0; ije1 < 2; ije1++) {
              for (j = 0; j < LX1; j++) {
                ig = idmo[ie][iface][ije1][1][LX1-1][j];
                tx[il] = tx[il] + qbnew[ije1][j][i-1]*tmor[ig]*0.5;
              }
            }
          }

          // if local edge 2 is a conforming edge
        } else {
          for (i = 1; i < LX1-1; i++) {
            il = idel[ie][iface][LX1-1][i];
            ig = idmo[ie][iface][0][0][LX1-1][i];
            tx[il] = tmor[ig];
          }
        }

        // if local edge 3 is a nonconforming edge
        if (idmo[ie][iface][0][0][LX1-1][0] != -1) {
          for (i = 1; i < LX1-1; i++) {
            il = idel[ie][iface][i][0];
            for (ije1 = 0; ije1 < 2; ije1++) {
              for (j = 0; j < LX1; j++) {
                ig = idmo[ie][iface][0][ije1][j][0];
                tx[il] = tx[il] + qbnew[ije1][j][i-1]*tmor[ig]*0.5;
              }
            }
          }
          // if local edge 3 is a conforming edge
        } else {
          for (i = 1; i < LX1-1; i++) {
            il = idel[ie][iface][i][0];
            ig = idmo[ie][iface][0][0][i][0];
            tx[il] = tmor[ig];
          }
        }
      }
    }
  }

   return 0;


 }

 static void calc(){

  int i,j, k, ntemp, temp, dtemp, temp1,temp2;
   int g1m1_s[6][5][5][5];
   int g4m1_s[6][5][5][5];

  for (i=0; i<5; i ++ )
	{
		xfrac[i]=((zgm1[i]*0.5)+0.5);
	}

  for (int isize=0; isize<6; isize ++ ){

    temp = pow(2.0, (-isize-2));
    dtemp = 1.0/temp;
    temp1 = temp*temp*temp;
    temp2 = temp*temp;
		for (k=0; k<5; k ++ ){
			for (j=0; j<5; j ++ ){
				for (i=0; i<5; i ++ )
		    {
					g1m1_s[isize][k][j][i]=(g1m1_s[isize][k][j][i]/wxm1[i]);
          g4m1_s[isize][k][j][i]=(g1m1_s[isize][k][j][i]/wxm1[i]);
				
				}
			}
		}
	}
  
 }

static int r_init(double a[], int n, double _const)
{
  int i;

  for (i = 0; i < n; i++) {
    a[i] = _const;
  }
  return 0;
}
