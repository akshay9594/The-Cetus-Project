int main () {

return 0;
}

int foo3()
{
  double a[10000][10000];
  int i,j;
  for ( i = 0; i <= 9998; i ++) {
    for ( j = 0; j <= 9999; j ++) {
      a[i][j] += a[i + 1][j];
    }
  }
  return 0;
}

void foo4(int x,int y)
{
  double a[10000][10000];
  int i, j;
  for ( i = 0; i <= 9998; i ++) {
    for ( j = 0; j <= 9999; j ++) {
      a[i][j] += a[i + 1][j];
    }
  }
}
