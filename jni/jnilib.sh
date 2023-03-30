#!/bin/bash -ev

jnilib=libsodiumjni.so
destlib=/usr/lib

echo $jnilib
echo $destlib
echo $destlib/$jnilib

SODIUM_LIB_DIR=../libsodium/libsodium-android-armv8-a/lib

gcc -I../libsodium/src/libsodium/include -I${JAVA_HOME}/include -I${JAVA_HOME}/include/linux -I${JAVA_HOME}/include/darwin sodium_wrap.c -shared -fPIC -L${SODIUM_LIB_DIR} -o $jnilib
