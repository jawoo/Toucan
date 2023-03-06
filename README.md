# Toucan

##1. Compiling DSSAT
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

##2. Cloning Toucan from this repo
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
