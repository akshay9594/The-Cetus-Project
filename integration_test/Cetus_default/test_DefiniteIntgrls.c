/* Trapezoidal Method definite integrals
*/
//#include<stdio.h>
//#include<math.h>

int main () {

	int i,n;
    float x0,xn,h,y[100],so,se,ans,x[100];
    
    //printf("\n Enter values of x0,xn,h:\n");
    //scanf("%f%f%f",&x0,&xn,&h);
    n=(xn-x0)/h;
    
    if(n%2==1) {
    	n=n+1;
    }
    h=(xn-x0)/n;
    
    //printf("\nrefined value of n and h are:%d  %f\n",n,h);
    //printf("\n Y values \n");
    
    for(i=0; i<=n; i++) {
        x[i]=x0+i*h;
        y[i]=(1/(1+pow(x[i],2)));
        //printf("\n%f\n",y[i]);
    }
    
    so=0;
    se=0;
    
    for(i=1; i<n; i++) {
        if(i%2==1) {
            so=so+y[i];
        } else {
            se=se+y[i];
        }
    }
    
    ans=h/3*(y[0]+y[n]+4*so+2*se);
    //printf("\nfinal integration is %f",ans);

	return 0;
}
