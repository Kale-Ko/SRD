# SRD

SRD is a WIP remote desktop suite written in Java with a custom C++ screen capture binding

## Development

### Required dependencies

#### Debian

Compiler:
`apt install gcc-x86-64-linux-gnu g++-x86-64-linux-gnu gcc-i686-linux-gnu g++-i686-linux-gnu gcc-aarch64-linux-gnu g++-aarch64-linux-gnu gcc-arm-linux-gnueabihf g++-arm-linux-gnueabihf`

Libraries:
`apt install libx11-dev:amd64 libxrandr-dev:amd64 libx11-dev:i386 libxrandr-dev:i386 libx11-dev:arm64 libxrandr-dev:arm64 libx11-dev:armhf libxrandr-dev:armhf`

Note: You must use `dpkg --add-architecture {arch}` for `amd64`, `i386`, `arm64`, `armhf`

Windows Compiler:
`apt install gcc-mingw-w64-x86-64-win32 g++-mingw-w64-x86-64-win32 gcc-mingw-w64-i686-win32 g++-mingw-w64-i686-win32`

#### Ubuntu

Ubuntu is the same as Debian except you need to add the ports repository.\
You may also need to manually specify `[arch=amd64,i386]` for the default repositories

`echo "deb [arch=arm64,armhf] http://ports.ubuntu.com/ubuntu-ports/ $(lsb_release -sc) main restricted universe multiverse" | sudo tee /etc/apt/sources.list.d/ubuntu-ports.list`

### Building

`./gradlew make`

then

`./gradlew build`