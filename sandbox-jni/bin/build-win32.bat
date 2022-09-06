@echo off

g++ -m64 -fpic -I%JAVA_HOME%/include -I%JAVA_HOME%/include/win32 -Ijavah -o ./libSandboxJniLibrary-win32.o -c ./src/libSandboxJniLibrary.cpp

g++ -shared -o ./libSandboxJniLibrary-win32.dll libSandboxJniLibrary-win32.o -W

@echo on