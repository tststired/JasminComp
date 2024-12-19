#!/bin/bash
cd ..
javac vc.java
cd Recogniser

for i in *.vc 
do
    echo "Running $i"
    java -Dstatus=true VC.vc $i
    echo 


done

