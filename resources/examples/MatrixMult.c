
int a[10000][10000], b[10000][10000], d[10000][10000];

int main(int argc, char const *argv[])
{

    int n = 10000, m = 10000;

    int i, j, k;
    // Matrix Multiplication kernel
    for (i = 0; i < 10000; i++)
    {

        for (j = 0; j < 10000; j++)
        {

            for (k = 0; k < 10000; k++)
            {

                d[i][j] = d[i][j] + a[i][k] * b[k][j];
            }
        }
    }

    return 0;
}
