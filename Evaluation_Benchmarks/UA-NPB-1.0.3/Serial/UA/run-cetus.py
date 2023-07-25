

import glob

import subprocess

import sys

from os.path import exists


print("Cetus Translation for UA NPB3.3.1 ...")

Cetus_path = '../../../../bin/cetus'

if(exists('npbparams.h') is False):
    print("Missing Input file: npbparams.h, first build the benchmark by typing 'make CLASS=A/B/C/D'")
    sys.exit()

test_files = 'ua.c convect.c diffuse.c adapt.c move.c mason.c precond.c utils.c transfer.c verify.c setup.c ../common/print_results.c ../common/c_timers.c ../common/wtime.c'

 
test_result = subprocess.call([Cetus_path, " -subsub_analysis -normalize-loops -alias=3 " + test_files],stdout=subprocess.DEVNULL,stderr=subprocess.STDOUT)
#
if(test_result != 0):
    print("Cetus Run Failed!! Check Error logs")

else:
    print("Cetus Run Succeeded!! Check cetus_output directory for the translated files...")