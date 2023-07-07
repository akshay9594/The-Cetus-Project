/*  Sieve of Eratosthenes
*/
//#include <string.h>

int main(){
	
    int prime[1001];
    //memset(prime, 1, sizeof(prime));
 
    for (int p=2; p*p<=1000; p++) {
        if (prime[p] == 1) {
            for (int i=p*2; i<=1000; i=i+p)
                prime[i] = 0;
        }
    }
	
   return 0;
}
