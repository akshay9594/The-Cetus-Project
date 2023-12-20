

import glob
import subprocess
import os
import platform


def run_integration_test(test_file):
    test_result = subprocess.call(['../../bin/cetus', '-paw-tiling', '-cacheSize=8192',
                                  '-cores=4', test_file], stdout=subprocess.DEVNULL, stderr=subprocess.PIPE)

    base_name = os.path.basename(test_file)
    base_name = base_name.split("test_").pop()
    validation_file_name = "case_" + base_name
    validation_file_path = os.path.join("./validation", validation_file_name)

    generated_file = os.path.join("./cetus_output", test_file)

    if platform.system() == "Windows":
        validation_command = ['fc', '/b', generated_file, validation_file_path]
    else:
        validation_command = ['diff', '-q',
                              generated_file, validation_file_path]

    test_result = subprocess.call(
        validation_command, stdout=subprocess.DEVNULL, stderr=subprocess.PIPE)

    return test_result


files = glob.glob("test_*.c")

val = input("Do you want to run all tests? Y or N: ")


if (val == 'Y'):
    print("Running all tests in this directory......")

    failed_tests = 0
    list_failed_tests = []

    for test_file in files:
        test_result = run_integration_test(test_file)
        if (test_result != 0):
            failed_tests = failed_tests + 1
            list_failed_tests.append(test_file)

    print("**********************************************\n")
    print("\nFound", len(files), "test cases")
    print("Passed = ", len(files) - failed_tests)

    print("Failed = ", failed_tests)

    if (list_failed_tests):
        print("Following tests failed:")

        for failed_test in list_failed_tests:
            print('  ', failed_test)

elif (val == 'N'):
    test_file = input("Specify a test file to run: ")
    test_result = run_integration_test(test_file)
    if (test_result != 0):
        print("Test for: ", test_file, " failed!")
    else:
        print("Test for: ", test_file, " passed!")
