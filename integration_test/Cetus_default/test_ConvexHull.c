/* Convex Hull Algorithm
*/

//#include<stdio.h>
//#include<string.h>
//#include<math.h>
 
#define max 10000000
#define min -10000000
 
typedef struct node
{
    double x;
    double y;
} node;
 
double orientation(node p, node q,node r) {
	double val = (q.y - p.y) * (r.x - q.x)-(q.x - p.x) * (r.y - q.y);
    if (val == 0)
    	return 0;
    if(val>0)
    	return 1;
    else
    	return 2;
}

int main() {
	long long int n;
    node points[10000];
    double x,y;
    //scanf("%lld",&n);
    int i;
 
    for(i=0;i<n;i++) {
    	//scanf("%lf",&x);
        //scanf("%lf",&y);
        points[i].x = x;
        points[i].y = y;
    }
 
    int  p = 0;
    int  l;
    for(i=1;i<n;i++) {
		if(points[p].x > points[i].x) {
        	p = i;
        } else if(points[p].x==points[i].x && points[p].y > points[i].y) {
        	p = i;
        }
    }
       
    l = p;
    int  q = (p+1)%n;
 
    node result[10001];
    int  count =0;
    if(n<3) {
    	for(i=0;i<n-1;i++) {
        	//printf("%lf %lf\n",points[i].x,points[i].y);
            result[count]=points[i];
            count++;
        }
    }
    else {
    	do	{
        	//printf("%lf %lf\n",points[p].x,points[p].y);
            result[count]=points[p];
            count++;
            for(i=0;i<n;i++) {
            	if(orientation(points[p],points[i],points[q])==2) {
                	q = i;
                }
            }
            p = q;
            q = (p+1) % n;
		} while(p!=l);
	}
 
    return 0;
}
 

