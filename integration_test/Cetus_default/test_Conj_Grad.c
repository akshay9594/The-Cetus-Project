/* 
	Computationally intensive loops extracted from the CG benchmark 
	of the NAS Parallel Benchmarks - C version (SNU NPB- 1.0.3)
*/

#define naa 15000

int main() {

int colidx[naa],rowstr[naa];
double x[naa],z[naa],a[naa],p[naa], q[naa], r[naa], *rnorm;
int firstrow = 1, lastrow=naa, firstcol=1, lastcol=naa;

int j, k;
int cgit, cgitmax = 25;
double d, sum, rho, rho0, alpha, beta;

  rho = 0.0;

  //---------------------------------------------------------------------
  // Initialize the CG algorithm:
  //---------------------------------------------------------------------
  for (j = 0; j < naa+1; j++) {
    q[j] = 0.0;
    z[j] = 0.0;
    r[j] = x[j];
    p[j] = r[j];
  }

  //---------------------------------------------------------------------
  // rho = r.r
  // Now, obtain the norm of r: First, sum squares of r elements locally...
  //---------------------------------------------------------------------
  for (j = 0; j < lastcol - firstcol + 1; j++) {
    rho = rho + r[j]*r[j];
  }

  //---------------------------------------------------------------------
  //---->
  // The conj grad iteration loop
  //---->
  //---------------------------------------------------------------------
  for (cgit = 1; cgit <= cgitmax; cgit++) {
    //---------------------------------------------------------------------
    // q = A.p
    // The partition submatrix-vector multiply: use workspace w
    //---------------------------------------------------------------------
    //
    // NOTE: this version of the multiply is actually (slightly: maybe %5) 
    //       faster on the sp2 on 16 nodes than is the unrolled-by-2 version 
    //       below.   On the Cray t3d, the reverse is true, i.e., the 
    //       unrolled-by-two version is some 10% faster.  
    //       The unrolled-by-8 version below is significantly faster
    //       on the Cray t3d - overall speed of code is 1.5 times faster.

 
    for (j = 0; j < lastrow - firstrow + 1; j++) {
      sum = 0.0;
      for (k = rowstr[j]; k < rowstr[j+1]; k++) {
        sum = sum + a[k]*p[colidx[k]];
      }
      q[j] = sum;
    }

    
    //---------------------------------------------------------------------
    // Obtain p.q
    //---------------------------------------------------------------------
    d = 0.0;
    for (j = 0; j < lastcol - firstcol + 1; j++) {
      d = d + p[j]*q[j];
    }

    //---------------------------------------------------------------------
    // Obtain alpha = rho / (p.q)
    //---------------------------------------------------------------------
    alpha = rho / d;

    //---------------------------------------------------------------------
    // Save a temporary of rho
    //---------------------------------------------------------------------
    rho0 = rho;

    //---------------------------------------------------------------------
    // Obtain z = z + alpha*p
    // and    r = r - alpha*q
    //---------------------------------------------------------------------
    rho = 0.0;
    for (j = 0; j < lastcol - firstcol + 1; j++) {
      z[j] = z[j] + alpha*p[j];
      r[j] = r[j] - alpha*q[j];
    }
            
    //---------------------------------------------------------------------
    // rho = r.r
    // Now, obtain the norm of r: First, sum squares of r elements locally...
    //---------------------------------------------------------------------
    for (j = 0; j < lastcol - firstcol + 1; j++) {
      rho = rho + r[j]*r[j];
    }

    //---------------------------------------------------------------------
    // Obtain beta:
    //---------------------------------------------------------------------
    beta = rho / rho0;

    //---------------------------------------------------------------------
    // p = r + beta*p
    //---------------------------------------------------------------------
    for (j = 0; j < lastcol - firstcol + 1; j++) {
      p[j] = r[j] + beta*p[j];
    }
  } // end of do cgit=1,cgitmax

  //---------------------------------------------------------------------
  // Compute residual norm explicitly:  ||r|| = ||x - A.z||
  // First, form A.z
  // The partition submatrix-vector multiply
  //---------------------------------------------------------------------
  sum = 0.0;

 
  for (j = 0; j < lastrow - firstrow + 1; j++) {
    d = 0.0;
    for (k = rowstr[j]; k < rowstr[j+1]; k++) {
      d = d + a[k]*z[colidx[k]];
    }
    r[j] = d;
  }

  //---------------------------------------------------------------------
  // At this point, r contains A.z
  //---------------------------------------------------------------------
  for (j = 0; j < lastcol-firstcol+1; j++) {
    d   = x[j] - r[j];
    sum = sum + d*d;
  }

}
