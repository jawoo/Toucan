# Toucan

## 1. Compiling DSSAT
(Assuming Amazon Linux 2)

```
sudo yum install gcc-gfortran
sudo yum install glibc-static
sudo yum install git
sudo yum install cmake
mkdir codebase
cd codebase
git clone https://github.com/dssat/dssat-csm-os
cd dscsm-csm-os
mkdir build
cd build
cmake ..
make
```
Now you have the DSSAT executable file ready to go!

## 2. Cloning Toucan from this repo
(Toucan is the cute name I gave to my Java program that batch-runs DSSAT)

```
cd ~/codebase
git clone https://github.com/jawoo/Toucan
cd Toucan
sudo yum install maven
mvn install
mvn compile
mvn package
```

## 3. Copy DSSAT files to the resource directory

```
cd ~/codebase/Toucan/res
mkdir -p .csm .temp result threads
cp ~/codebase/dssat-csm-os/Data/* ./.csm
cp ~/codebase/dssat-csm-os/Data/Genotype/* ./.csm
cp ~/codebase/dssat-csm-os/Data/Pest/* ./.csm
cp ~/codebase/dssat-csm-os/Data/StandardData/* ./.csm
cp ~/codebase/dssat-csm-os/build/bin/dscsm048 ./.csm/DSCSM048.EXE
```

## 4. Flag which cultivar to use in the simulation
You'll need to flag in the cultivar file (*.CUL) to tell the program which cultivar to use. Open the cultivar file for the crop you'd like to simulation (e.g., MZCER.048.CUL) in the res/.csm directory and add space and an asterisk at the end of the line, like the following:

```
990002 MEDIUM SEASON        . IB0001 200.0 0.300 800.0 700.0  8.50 38.90 *
```
You can flag as many cultivars as you like.

## 5. Now you can run Toucan!

```
cd ~/codebase/Toucan/
java -cp "target/ToucanSNX-1.0-SNAPSHOT.jar:lib/*" org.cgiar.toucan.App
```
