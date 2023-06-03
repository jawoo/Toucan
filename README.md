# Toucan

## 1. Compiling DSSAT
Amazon Linux 2:
```
sudo yum install gcc-gfortran
sudo yum install glibc-static
sudo yum install git
sudo yum install maven
sudo yum install cmake
mkdir codebase
cd codebase
git clone https://github.com/dssat/dssat-csm-os
cd dscsm-csm-os
mkdir release
cd release
cmake -DCMAKE_BUILD_TYPE=Release ..
make
```
Or, Ubuntu:
```
sudo apt install gfortran
sudo apt install cmake
sudo apt install maven
mkdir codebase
cd codebase
git clone https://github.com/dssat/dssat-csm-os
cd dscsm-csm-os
mkdir release
cd release
cmake -DCMAKE_BUILD_TYPE=Release ..
make
```
Now you have the DSSAT executable file ready to go.

## 2. Cloning Toucan from this repo
(Toucan is the cute name I gave to my Java program that batch-runs DSSAT)

```
cd ~/codebase
git clone https://github.com/jawoo/Toucan
cd Toucan
mvn clean compile assembly:single
```

## 3. Copy DSSAT files to the resource directory
For the sake of simplicity, I keep *all* files into one directory with no subdirectories.
```
cd ~/codebase/Toucan/res
mkdir -p .csm .temp result threads
cp ~/codebase/dssat-csm-os/Data/* ./.csm
cp ~/codebase/dssat-csm-os/Data/Genotype/* ./.csm
cp ~/codebase/dssat-csm-os/Data/Pest/* ./.csm
cp ~/codebase/dssat-csm-os/Data/StandardData/* ./.csm
cp ~/codebase/dssat-csm-os/release/bin/dscsm048 ./.csm/DSCSM048.EXE
```
And, while we're at it, also create some directories to collect and process temporary files.
```
cd ~/codebase/Toucan/res/.temp
mkdir summary multipleplanting flowering planting error
```

## 4. Flag which cultivar to use in the simulation
You'll need to flag in the cultivar file (*.CUL) to tell the program which cultivar to use. Open the cultivar file for the crop you'd like to use (e.g., MZCER.048.CUL) in the res/.csm directory and add a space and an asterisk at the end of the line, like the following:

```
990002 MEDIUM SEASON        . IB0001 200.0 0.300 800.0 700.0  8.50 38.90 *
```
You can flag as many cultivars as you like.

## 5. Run Toucan

```
cd ~/codebase/Toucan/
java -jar target/ToucanSNX-1.0-SNAPSHOT-jar-with-dependencies.jar
```
The values of model input parameters are defined in the "config.yml" file in the root directory.

After all the batch runs are completed, you can pick up the merged CSV output file at ~/codebase/Toucan/res/result directory. Enjoy!
