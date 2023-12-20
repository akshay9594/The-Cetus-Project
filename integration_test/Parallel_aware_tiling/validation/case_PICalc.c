/*
Copyright (C) 1991-2022 Free Software Foundation, Inc.
   This file is part of the GNU C Library.

   The GNU C Library is free software; you can redistribute it andor
   modify it under the terms of the GNU Lesser General Public
   License as published by the Free Software Foundation; either
   version 2.1 of the License, or (at your option) any later version.

   The GNU C Library is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
   Lesser General Public License for more details.

   You should have received a copy of the GNU Lesser General Public
   License along with the GNU C Library; if not, see
   <https:www.gnu.org/licenses/>. 
*/
/*
This header is separate from features.h so that the compiler can
   include it implicitly at the start of every compilation.  It must
   not itself include <features.h> or any other header that includes
   <features.h> because the implicit include comes before any feature
   test macros that may be defined in a source file before it first
   explicitly includes a system header.  GCC knows the name of this
   header in order to preinclude it. 
*/
/*
glibc's intent is to support the IEC 559 math functionality, real
   and complex.  If the GCC (4.9 and later) predefined macros
   specifying compiler intent are available, use them to determine
   whether the overall intent is to support these features; otherwise,
   presume an older compiler has intent to support these features and
   define these macros by default. 
*/
/*
wchar_t uses Unicode 10.0.0.  Version 10.0 of the Unicode Standard is
   synchronized with ISOIEC 10646:2017, fifth edition, plus
   the following additions from Amendment 1 to the fifth edition:
   - 56 emoji characters
   - 285 hentaigana
   - 3 additional Zanabazar Square characters
*/
/*
PI calculation


*/
/* #include <stdlib.h> */
/* #include <stdio.h> */
/* #include <time.h> */
int inside_circle(double x, double y)
{
	int rad = 1;
	int circle_x = 1, circle_y = 1;
	int _ret_val_0;
	if ((((x-circle_x)*(x-circle_x))+((y-circle_y)*(y-circle_y)))<=(rad*rad))
	{
		_ret_val_0=1;
		return _ret_val_0;
	}
	else
	{
		_ret_val_0=0;
		return _ret_val_0;
	}
	return _ret_val_0;
}

int main()
{
	int npoints = 10000;
	int circle_count = 0;
	int j;
	double PI;
	int _ret_val_0;
	srand(time(NULL));
	#pragma loop name main#0 
	#pragma cetus private(j, xcoordinate, ycoordinate) 
	/* #pragma cetus reduction(+: circle_count)  */
	for (j=1; j<=npoints; j ++ )
	{
		double xcoordinate = (((double)rand())/((double)RAND_MAX))*2.0;
		double ycoordinate = (((double)rand())/((double)RAND_MAX))*2.0;
		/*
		
		
		  	printf("%f ",xcoordinate);
		
		  	printf("- %f",ycoordinate);
		
		  	printf(" val: %d\n",inside_circle(xcoordinate, ycoordinate));
		
		  	
		*/
		if (inside_circle(xcoordinate, ycoordinate)==1)
		{
			circle_count=(circle_count+1);
		}
	}
	PI=((4.0*circle_count)/npoints);
	/* printf ("%f\n",PI); */
	_ret_val_0=0;
	return _ret_val_0;
}
