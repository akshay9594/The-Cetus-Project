/* PI calculation
*/
//#include <stdlib.h>
//#include <stdio.h>
//#include <time.h>

int inside_circle (double x, double y) {

  int rad = 1;
  int circle_x = 1, circle_y = 1;
    if ((x - circle_x) * (x - circle_x) +
        (y - circle_y) * (y - circle_y) <= rad * rad) {
        return 1;
    } else {
        return 0;
    }
}

int main () {

int npoints = 10000;
int circle_count = 0;
int j;
double PI;

srand(time(NULL));
	
for (j=1; j<=npoints; j++) {

  	double xcoordinate = (double)rand() / (double)RAND_MAX * 2.0;
  	double ycoordinate = (double)rand() / (double)RAND_MAX * 2.0;
  	
  	/*
  	printf("%f ",xcoordinate);
  	printf("- %f",ycoordinate);
  	printf(" val: %d\n",inside_circle(xcoordinate, ycoordinate));
  	*/
  	
  	if (inside_circle(xcoordinate, ycoordinate)==1) {
		circle_count = circle_count + 1;
	}
}

PI = 4.0*circle_count/npoints;

//printf ("%f\n",PI);

return 0;
}
