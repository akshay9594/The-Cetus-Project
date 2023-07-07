

import glob

import subprocess


print("Cetus Translation for AMGmk v1.0...")

Cetus_path = '/home/akshay/Cetus-SubSub/bin/cetus'

test_files = 'csr_matrix.c csr_matvec.c hypre_error.c hypre_memory.c laplace.c main.c relax.c vector.c'

 
test_result = subprocess.call([Cetus_path, " -subsub_analysis -normalize-loops -alias=3 " + test_files],stdout=subprocess.DEVNULL,stderr=subprocess.STDOUT)
 
if(test_result != 0):
    print("Cetus Run Failed!! Check Error logs")

else:
    print("Cetus Run Succeeded!! Check cetus_output directory for the translated files...")