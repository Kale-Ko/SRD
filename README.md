# SRD

SRD is a WIP remote desktop suite written in Java with a custom C++ screen capture binding

## Development

### Required dependencies

#### Debian/Ubuntu

Compiler:
`apt install gcc-x86-64-linux-gnu g++-x86-64-linux-gnu g++-mingw-w64-x86-64-win32 g++-mingw-w64-x86-64-win32`

Libraries:
`apt install libx11-dev libxrandr-dev`

### Building

`./gradlew build`