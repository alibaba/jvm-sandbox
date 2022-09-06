#!/bin/bash

# compile
g++ -arch x86_64 -arch arm64 -fpic -I${JAVA_HOME}/include -I${JAVA_HOME}/include/darwin -Ijavah -o ./libSandboxJniLibrary-darwin.o -c ./src/libSandboxJniLibrary.cpp

# link
g++ -dynamiclib -o ./libSandboxJniLibrary-darwin.dylib libSandboxJniLibrary-darwin.o -lc