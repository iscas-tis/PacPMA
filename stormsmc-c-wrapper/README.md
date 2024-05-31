# How to compile stormsmc-c-wrapper
The compilation of `stormsmc-c-wrapper` is based on CMake. 
Since `stormsmc-c-wrapper` is built on top of `storm`, it needs to locate the `storm` libraries. 
This can be achieved by adding the `build` directory of `storm` to the variable `CMAKE_PREFIX_PATH`, for instance by the command

```
export CMAKE_PREFIX_PATH=<path to storm directory>/build:${CMAKE_PREFIX_PATH}
```

Then, to compile `stormsmc-c-wrapper`, just run the commands

```
mkdir build
cd build
cmake ..
make
```
