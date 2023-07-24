# How-to run PAC Model checker

## Compiling PAC Model checker

To compile PAC Model checker, make sure that JAVA 17 JDK (or later) and maven are installed and working correctly.
Then, use the `build.sh` script to generate the `pacpma.jar` archive.

## Running PAC Model checker

Once `pacpma.jar` has been generated correctly, use the command 

```
java -jar pacpma.jar --help
```
to run the PAC Model checker and see the options it accepts. 
The usual invocation to check the property *P=? [F "error"]* against the parametric model *model.prism* with parameter *pc* ranging between 0.25 and 0.75 is

```
java -jar pacpma.jar -f model.prism -p 'P=? [F "error"]' -P pc=0.25:0.75
```

## Runtime dependencies of PAC Model checker

PAC Model checker has the following runtime dependencies, according to the options used to call it.

- Model checker executable (option: `--model-checker`):
  * `storm`
  * `storm-c-wrapper`
  * `storm-python.py`
  
  One of these executable files need to be found in `$PATH`, unless its path is provided through the option `--model-checker-path`
- LP solver (option: `--lpsolver`):
  * `octave`
  * `lpsolve`
  
  Either the `octave` executable needs to be available in `$PATH`, or the library `lpsolve55` needs to be available in a directory known to `ld.so`
