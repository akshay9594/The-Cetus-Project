

import glob

import subprocess

files = glob.glob("test_*.c")

val = input("Do you want to run all tests? Y or N: ")

if(val == 'Y'):
    print("Running all tests in this directory......")

    failed_tests = 0
    list_failed_tests = []

    for test_file in files:
        test_result = subprocess.call(['../../bin/cetus', " -subsub_analysis -normalize-loops " + test_file],stdout=subprocess.DEVNULL,stderr=subprocess.STDOUT)
        if(test_result != 0):
            failed_tests = failed_tests + 1
            list_failed_tests.append(test_file)


    print("**********************************************\n")
    print("\nFound" , len(files), "test cases")
    print("Passed = ", len(files) - failed_tests)

    print("Failed = " , failed_tests)

    if(list_failed_tests):
        print("Following tests failed:")

        for failed_test in list_failed_tests:
            print('  ' , failed_test)

elif(val == 'N'):
    test_file = input("Specify the test file: ")
    test_result = subprocess.call(['../../bin/cetus', " -subsub_analysis -normalize-loops -parallelize-loops=3 " + test_file])
    if(test_result != 0):
        print("Test for: ", test_file , " failed!")
    else:
         print("Test for: ", test_file , " passed!")