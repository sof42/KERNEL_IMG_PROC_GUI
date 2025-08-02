# Kernel Image Processing Project

This project implements kernel-based image processing techniques using three different computing approaches. Developed by **Sofija Kochovska** as part of academic research at FAMNIT, University of Primorska.


## Features

### Implementation Approaches

| Approach   | Technology   | Description                |
|------------|--------------|----------------------------|
| Sequential | Single-thread| Baseline implementation    |
| Parallel   | ForkJoinPool | Multi-threaded optimization|
| Distributed| MPJ Express  | MPI-based cluster processing|

### Supported Operations
- Grayscale conversion  
- Kernel convolution:  
  - Ridge Detection
  - Sharpening  
  - Edge detection  
  - Identity
  - Custom Kernel  

## Requirements

### Software
- Java 21+ (OpenJDK recommended)  
- MPJ Express v0.44+ (for distributed version)  


## Setup and Usage

### MPJ Express Setup (for distributed version)

Download MPJ Express from [mpj-express.org](http://mpj-express.org).

Extract and set environment variables:

```bash
export MPJ_HOME=/path/to/mpj-express
export PATH=$MPJ_HOME/bin:$PATH
```
### Running the Project

First, ensure you are in the project directory:

```bash
cd /path/to/kernel-img-processing
```
Then navigate to the source directory:

```bash
cd src
```
### Sequential Version

```bash
javac sequential/*.java
java sequential.Main
```
### Parallel Version
```bash
javac parallel/*.java
java parallel.Main
```
### Distributed Version
The results in this project were obtained using 30 processes.
```bash
javac -cp .:$MPJ_HOME/lib/mpj.jar distributed/*.java
$MPJ_HOME/bin/mpjrun.sh -np <number_of_processes> -cp . distributed.Main
```

