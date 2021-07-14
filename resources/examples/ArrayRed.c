/* Array Reduction

  The loop contains an array reduction operation, 
  a.k.a irregular reduction or histogram reduction

*/
int main(){

  float a[1000], sum[1000];
  int i, n, tab[1000];
  
  /* define content of sum and a */

  for (i=1; i<1000; i++) {
    sum[tab[i]] = sum[tab[i]] + a[i];
  }
	
   return 0;
}
