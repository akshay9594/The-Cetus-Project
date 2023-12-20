

import glob

import subprocess


print("Cetus Translation for CHOLMOD Supernodal...")

Cetus_path = '/home/akshay/The-Cetus-Project/bin/cetus'

test_files = 'cholmod_super_numeric.c'

 
test_result = subprocess.call([Cetus_path, " -subsub_analysis -normalize-loops -alias=3 " + test_files],stdout=subprocess.DEVNULL,stderr=subprocess.STDOUT)
 
if(test_result != 0):
    print("Cetus Run Failed!! Check Error logs")

else:
    print("Cetus Run Succeeded!! Check cetus_output directory for the translated files...")