#!/bin/bash

typeset DEFAULT_JAVA_HOME='/opt/taobao/java';

if [ ! "${JAVA_HOME}" ]; then
  JAVA_HOME=${DEFAULT_JAVA_HOME}
fi

# compile
g++ -m64 -fpic -I${JAVA_HOME}/include -I${JAVA_HOME}/include/linux -Ijavah -o ./libSandboxJniLibrary-linux.o -c ./src/libSandboxJniLibrary.cpp
# link
g++ -shared -fPIC -o ./libSandboxJniLibrary-linux.so libSandboxJniLibrary-linux.o -lc