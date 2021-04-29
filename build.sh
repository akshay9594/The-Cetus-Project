#!/bin/sh
#
# build.sh - build script for Cetus.
#

# Specify the location of the antlr.jar file for your system.
ANTLR="$PWD/lib/antlr.jar"            #antlr location
#please put antlr.jar under lib/
#otherwise you must change the path here 
#and modify MANIFEST-ADD.MF to make sure the path of antlr.jar
#is relative to cetus.jar

if [ -z "$ANTLR" ]; then
  echo "Please define ANTLR in $0"
  exit 1
fi

# Check for java/javac/jar.
for tool in java javac jar; do
  which $tool >/dev/null
  if [ $? -ne 0 ]; then
    echo $tool is not found.
    exit 1
  fi
done

# No change is required for these variables.
CETUSROOT=$PWD
SRC="$CETUSROOT/src/*/*/*.java $CETUSROOT/src/*/*/*/*.java"
PARSER="$CETUSROOT/src/cetus/base/grammars"
# Source files for parser construction.
parser_src="
  CetusCParser.java
  CToken.java
  LineObject.java
  NewCParser.g
  Pragma.java
  Pre.g
  PreprocessorInfoChannel.java
"

case "$1" in
  parser)
  echo "Compiling the parser using ANTLR..."
  #Build the parser
  cd $PARSER
  java -cp $ANTLR:class antlr.Tool $PARSER/Pre.g
  java -cp $ANTLR:class antlr.Tool $PARSER/NewCParser.g
  cd -
  ;;
  compile)
  echo "Compiling the source files..."
  [ -f $PARSER/NewCParser.java ] || $0 parser
  [ -d class ] || mkdir class
  javac -g -cp $PWD/lib/junit.jar:$PWD/lib/rsyntaxtextarea.jar:$ANTLR:class -d class $SRC
  ;;
  jar)
  $0 compile
  echo "Archiving the class files..."
  [ -d lib ] || mkdir lib
  jar cfm lib/cetus.jar MANIFEST-ADD.MF -C class .
  ;;
  gui)
  $0 compile
  echo "Archiving the gui files..."
  [ -d lib ] || mkdir lib
  jar cfm lib/cetusgui.jar MANIFEST-ADD-GUI.MF -C class . lib/rsyntaxtextarea.jar -C resources .
  ;;
  javadoc)
  echo "Generating JAVA documents..."
  javadoc -d api $SRC -classpath $ANTLR:$PWD/lib/junit.jar:$PWD/lib/rsyntaxtextarea.jar
  ;;
  clean)
  echo "Cleaning up..."
  rm -rf class bin lib/cetus*.jar
  ;;
  bin)
  $0 jar
  echo "Generating a wrapper..."
  [ -d bin ] || mkdir bin
  classpath="$PWD/lib/cetus.jar:$PWD/lib/rsyntaxtextarea.jar"
  cat > bin/cetus << EOF
#!/bin/sh
# This file was generated automatically by build.sh.
java -cp $classpath -Xmx1g cetus.exec.Driver \$*
EOF
  chmod 755 bin/cetus
  ;;
  *)
  echo "Usage: $0 <target>"
  echo "  <target> is one of the followings:"
  echo "  bin     - compile, jar, and generate a wrapper script."
  echo "  compile - compile the source files."
  echo "  clean   - remove classes, jar, and the wrapper."
  echo "  jar     - archive the class files."
  echo "  gui     - archive the class files to create the GUI version."
  echo "  javadoc - generate api documents."
  echo "  parser  - rebuild the parser."
  exit 1
  ;;
esac
