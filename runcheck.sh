#!/bin/bash
javac vc.java
cd Checker/tests

# Define ANSI color codes
GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m' # No Color


for i in `ls *me` 
do 
    rm $i
done 

for i in `ls *filtered` 
do 
    rm $i
done 

for i in `ls test*` 
do
    num="${i#test}"
    echo $i
    java VC.vc "test$num" > "tmp"
    cat  "tmp" "$i" > "result$num.me"
    diff "result$num.me" "result$num"


done