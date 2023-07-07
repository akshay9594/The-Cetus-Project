/* Runge-Kutta method
*/
//#include <stdio.h>
//#include <stdlib.h>
//#include <math.h>

/*
double rk4(double dx, double x, double y)
{
	double	k1 = dx * (x) * sqrt (y),
		k2 = dx * (x + dx / 2) * sqrt (y + k1 / 2),
		k3 = dx * (x + dx / 2) * sqrt (y + k2 / 2),
		k4 = dx * (x + dx) * sqrt (y + k3);
	return y + (k1 + 2 * k2 + 2 * k3 + k4) / 6;
}
*/

int main () {
	
	double y[1005], x, y2;
	double x0 = 0, x1 = 10, dx = .1;
	int i, n = 1 + (x1 - x0)/dx;
	
 	y[0] = 1;
	for (i = 1; i < n; i++) {
		double 	xs = x0 + dx * (i - 1);
		double  ys = y[i-1];
		double	k1 = dx * (xs) * sqrt (ys),
				k2 = dx * (xs + dx / 2) * sqrt (ys + k1 / 2),
				k3 = dx * (xs + dx / 2) * sqrt (ys + k2 / 2),
				k4 = dx * (xs + dx) * sqrt (ys + k3);	
		y[i] = ys + (k1 + 2 * k2 + 2 * k3 + k4) / 6;
 	}
 	
	//printf("x\ty\trel. err.\n------------\n");
	for (i = 0; i < n; i += 10) {
		x = x0 + dx * i;
		y2 = pow(x * x / 4 + 1, 2);
		//printf("%g\t%g\t%g\n", x, y[i], y[i]/y2 - 1);
	}
 	
	return 0;
}
