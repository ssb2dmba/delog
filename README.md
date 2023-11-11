# Delog 

## An Android Kotlin JetPack Compose Room Koin Material3 Secure Scuttlebutt (SSB) Application

- [x] multiple identities
- [x] Secret Handshake Protocol by Dominic Tar boworred from [Apache Tuweni](https://github.com/apache/incubator-tuweni)
- [x] follow-bot invite consume
- [x] basic network features (send, receive,)
- [x] Android Kotlin JetPack Compose modern app
- [x] Material3
- [x] Room Database
- [x] [Koin](https://insert-koin.io/) dependency injection
- [x] multiple identities
- [x] BIP39 export mnemonic
- [x] Espresso/Gherkin Behavioral Driven Integration Tests
- [x] BIP39 import/export mnemonic

- [ ] blob support
- [x] hypertext link embedding
- [.] quotes, reply, vote support
- [x] Markdown support
- [x] SSB protocol improvement proposals
- [ ] draft ordering & support
- [x] Tor low-power-integration
- [ ] ...

## Build

Project build well with Android Studio Electric Eel.

You will need an Android NDK and set in the Android Studio env i.e.: 
```
~$ export ANDROID_NDK_HOME=~/Android/Sdk/ndk/25.1.8937393/
~$ ./android-studio/bin/studio.sh
```

## Running tests

Given `cucumberUseAndroidJUnitRunner` in `gradle.properties` you can run cucumber-android 
bdd tests (default) or JUnit4 classic `connectedAndroidTest`.
