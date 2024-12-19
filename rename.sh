#!/bin/bash

for i in {1..9}; do
    mv "t$i.vc" "t0$i.vc"
    mv "t$i.sol" "t0$i.sol"
    mv "t$i.vcu" "t0$i.vcu"
    mv "t$i.vct" "t0$i.vct"
    mv "t$i.vcuu" "t0$i.vcuu"
    mv "t$i.vcut" "t0$i.vcut"
done