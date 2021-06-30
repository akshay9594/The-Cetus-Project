#include <stdio.h>
#include <time.h>
//gcc -o fractales fractales.c  
//gcc -o fractales1 cetus_output/fractales.c  
//Calculate if the point belongs to Malderbort's set
int calc_frac_point(float cx,float cy,int max_itr)
{
   int itr;
   double x = 0.0, y = 0.0, xx = 0.0, yy= 0.0, xy;
   for(itr = 1; (itr < max_itr) && ((xx + yy) < 4.0); itr++)
   {
      xx = x * x;
      yy = y * y;
      xy = x * y;
      x = xx - yy + cx;
      y = 2*xy + cy;
   }
   return itr;
}

//Assign color to the pixel
int assign_color(int itr, int max_itr)
{
   if(itr == max_itr)
      return 10;
   else
      return 255;
}

int main(){
   int Xmax = 480, Ymax = 480;
   float cx0 = -2.0;
   float cy0 = -2.0;
   float cxn = 2.0;
   float cyn = 2.0;
   int max_itr= 10000;
   printf("%i %f %f %f %f %i\n",Ymax, cx0,cxn,cy0,cyn,max_itr);
   float deltax = (cxn - cx0)/Xmax;
   float deltay = (cyn - cy0)/Ymax;
   float cx, cy = cyn;
   int color,itr,i,j;
   FILE *fp = fopen ("fractales.ppm", "w+");
   fprintf( fp, "P2\n%d %d\n%d\n", Xmax, Ymax, 255);
   // Calculate the time taken by fun()
   clock_t t = clock();
   for (j = 0; j < Ymax; j++)    //Horizontal size, Y coordinates: columns
   {
      cx = cx0;
      for(i = 0; i < Xmax; i++) //Vertical size, X coordinates: rows
      {
         //Calculate iteration
         itr = calc_frac_point(cx, cy, max_itr);
         //Calculate color contribution
         color = assign_color(itr, max_itr);
         //Write it into a file
         fprintf(fp, "%i ", color);
         cx = cx + deltax;
      }
      cy = cy-deltay;
   }
   t = clock() - t;
   double time_taken = ((double)t)/CLOCKS_PER_SEC; // in seconds
   printf("The process took %f seconds to execute \n", time_taken);
   return 0;
} /* main */
