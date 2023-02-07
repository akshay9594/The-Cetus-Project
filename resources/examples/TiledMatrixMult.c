int a[10000][10000], b[10000][10000], d[10000][10000];
int main(int argc, char const *argv[])
{
    int n = 10000, m = 10000;
    int i, j, k, l;
    int _ret_val_0;
    int i_tiling1;
    int j_tiling1;
    int k_tiling1;
    for ((k_tiling1 = 0); k_tiling1 < n; k_tiling1 += 2048)
    {
        for ((j_tiling1 = 0); j_tiling1 < m; j_tiling1 += 2048)
        {
            for ((i_tiling1 = 0); i_tiling1 < n; i_tiling1 += 2048)
            {
                for ((i = i_tiling1); i < (((2047 + i_tiling1) < n) ? (2047 + i_tiling1) : n); i++)
                {
                    for ((j = j_tiling1); j < (((2047 + j_tiling1) < m) ? (2047 + j_tiling1) : m); j++)
                    {
                        for ((k = k_tiling1); k < (((2047 + k_tiling1) < n) ? (2047 + k_tiling1) : n); k++)
                        {
                            d[i][j] = (d[i][j] + (a[i][k] * b[k][j]));
                        }
                    }
                }
            }
        }
    }
    _ret_val_0 = 0;
    return _ret_val_0;
}
