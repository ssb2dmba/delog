#!/bin/bash -ev

jnilib=libsodiumjni.so
destlib=/usr/lib

echo $jnilib
echo $destlib
echo $destlib/$jnilib

#sudo cp /usr/local/lib/libsodium.* /usr/lib

SODIUM_LIB_DIR=../libsodium/libsodium-android-armv8-a/lib

gcc -I../libsodium/src/libsodium/include -I${JAVA_HOME}/include -I${JAVA_HOME}/include/linux -I${JAVA_HOME}/include/darwin sodium_wrap.c -shared -fPIC -L${SODIUM_LIB_DIR} -o $jnilib
#sudo rm -f $destlib/$jnilib
#sudo cp $jnilib $destlib
#sudo cp ${SODIUM_LIB_DIR}/libsodium.so /usr/lib