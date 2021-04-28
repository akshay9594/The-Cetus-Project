/* Array Privatization Example

  The variable t is an array used temporarily during a single iteration of the
  outer loop. No value of t is used in an iteration other than the one that
  produced it. Without privatization, executing different iterations in
  parallel would create conflicts on accesses to t.  Declaring t private gives
  each thread a separate storage space, avoiding these conflicts.

*/
int main(){

  float a[1000][1000], b[1000][1000], t[1000];
  int i, j;
  
  for (i=1; i<1000; i++) {
     for (j=1; j<1000; j++) {
       t[j] = a[i][j]+b[i][j];
     }
     for (j=1; j<1000; j++) {
       b[i][j] =  t[j] + sqrt(t[j]);
     }
  }
	
   return 0;
}
