-------------------------------------------------------------------------------
RELEASE
-------------------------------------------------------------------------------

Cetus 1.4.4 version (Jan 2017) introduces new methods for setting the stride 
information of an array access, allowing more precise analysis of array accesses.

Cetus 1.4.3 version (May 2016) contains minor changes that fixed some Java 8
specific bugs which disallowed Cetus from building.

Cetus 1.4.2 version (Dec 2014), unlike its predecessor (1.4.1), is not a binary 
release. It is a complete source release for both Cetus command line version and
the GUI version.

Cetus is a source-to-source compiler infrastructure for C written in Java, and
can be downloaded from http://cetus.ecn.purdue.edu. This version contains a 
graphic user interface (GUI), a client-server mode and contains minor updates 
and fixes in the existing passes.

-------------------------------------------------------------------------------
FEATURES/UPDATES
-------------------------------------------------------------------------------
* New features in 1.4.4

Cetus 1.4.4 adds two new methods, setStride() and getStride(), to the RangeExpression 
class for setting and getting stride information of an array access. These new 
methods allow the stride of the access, which is 1 by default, to be set. This allows 
more precise analysis of array accesses in the program.

The following example shows the difference between the new and old RangeExpression.

for (int i = 1; i < 10; i += 2)

... = A[i]

The access range for array A is [1:10:2] (1 to 10 with stride 2.) Using the new 
RangeExpression, this access can be represented by 

RangeExpression(new IntegerLiteral(1), new IntegerLiteral(10), new IntegerLiteral(2)) 

Which is a precise representation of these accesses. The old RangeExpression would 
use the representation 

RangeExpression(new IntegerLiteral(1), new IntegerLiteral(9)). 

This is equivalent to [1:9], which overestimates the accesses of array A. 

An example usage of the stride information in a compiler analysis can be seen in 
the Section class where the precise intersection between RangeExpressions with 
constant stride is implemented. Additional examples can be seen in the unit testing
for the new RangeExpression and Section classes (TestTripletCases.java).

In summary:

- Stride information is added to RangeExpression. It can be read and set using 
  the getStride() and setStride() methods.
- The new RangeExpressions constructor is backward compatible with the previous 
  version. All RangeExpression with unspecified stride will be treated as having 
  a stride value of 1.
- A precise intersection operation is implemented for Section class.

-------------------------------------------------------------------------------
CONTENTS
-------------------------------------------------------------------------------
This Cetus release has the following contents.

  lib            - Archived classes (jar)
  src            - Cetus source code
  antlr_license.txt - ANTLR license
  RSyntaxTextArea.License.txt - RSyntaxTextArea license
  cetus_license.txt    - Cetus license
  build.sh       - Command line build script
  build.xml      - Build configuration for Apache Ant
  readme.txt     - This file
  readme_log.txt - Archived release notes
  readme_omp2gpu.txt - readme file for OpenMP-to-CUDA translator

-------------------------------------------------------------------------------
REQUIREMENTS
-------------------------------------------------------------------------------
* JAVA SE 6
* ANTLRv2
* GCC (Cygwin GCC-4 for Windows OS)

-------------------------------------------------------------------------------
INSTALLATION
-------------------------------------------------------------------------------
* Obtain Cetus distribution
  The latest version of Cetus can be obtained at:
  http://cetus.ecn.purdue.edu/

* Binary Version
  For binary version (.jar) of Cetus, installation is not needed.

* Unpack
  Users need to unpack the distribution before installing Cetus.
  $ cd <directory_where_cetus.tar.gz_exists>
  $ gzip -d cetus.tar.gz | tar xvf -

* Build
  There are several options for building Cetus:
  - For Apache Ant users
    The provided build.xml defines the build targets for Cetus. The available
    targets are "compile", "jar", "gui", "clean" and "javadoc". Users need to edit
    the location of the Antlr tool.
  - For Linux/Unix command line users
    Run the script build.sh after defining system-dependent variables in the
    script.
  - For SDK (Eclipse, Netbeans, etc) users
    First, build the parser with the Antlr tool.
    Then, follow the instructions of each SDK to set up a project.

-------------------------------------------------------------------------------
RUNNING CETUS
-------------------------------------------------------------------------------
Users can run Cetus in the following way:

  $ java -classpath=<user_class_path> cetus.exec.Driver <options> <C files>

The "user_class_path" should include the class paths of Antlr, rsyntaxtextarea and Cetus.
"build.sh" and "build.xml" provides a target that generates a wrapper script
for Cetus users.

- Like previous versions, you can still run command line version of Cetus by running "cetus" or "java -jar
cetusgui.jar" plus flags (options) and input C file, e.g. "cetus foo.c" or "java -jar cetusgui.jar foo.c" for
processing C file with the default options.

  - You can start Cetus GUI by double-clicking cetusgui.jar if your OS supports it. You can also start Cetus GUI by
running "java -jar cetusgui.jar" or "java -jar cetusgui.jar -gui" in command line. Starting Cetus GUI through command
line should work on all Windows, Linux and Mac. Previous script "cetus" is still working and "cetus gui" starts GUI
too.

  - If you want to process your C code by Cetus on Windows, a preprocessor, i.e. Cygwin gcc-4.exe and cpp-4.exe), must be
installed. However, compiling C code by Cetus with Cygwin on Windows has not been fully tested and is not guaranteed to
work. Also, after installing Cygwin on Windows, the path to gcc-4.exe and cpp-4.exe (e.g. C:\cygwin\bin) must be set in Environment
Variables on Windows.
  - The binary version is only for personal and academic use, not for commercial use. The license files of ANTLR and
Cetus are also included in cetusgui.jar. We will regularly update the binary version. Please always visit Cetus website
and download the latest version.

-------------------------------------------------------------------------------
KNOWN ISSUES
-------------------------------------------------------------------------------
Starting 1.4.0, the automatic loop parallelization was turned ON by default (level 1). We have seen that this
may lead to translation failures in certain cases. One such case is of usage of typedef'ed variables in the
potential parallel loops. We are working towards fixing this issue.


-------------------------------------------------------------------------------
TESTING
-------------------------------------------------------------------------------
We have tested Cetus successfully using the following benchmark suites:

* SPEC CPU2006
  More information about this suite is available at http://www.spec.org

* SPEC OMP2001
  More information about this suite is available at http://www.spec.org

* NPB 2.3 written in C
  More information about this suite is available at
  http://www.hpcs.cs.tsukuba.ac.jp/omni-openmp/

May, 2013
The Cetus Team

URL: http://cetus.ecn.purdue.edu
EMAIL: cetus@ecn.purdue.edu




