#!/bin/bash
#
# jasmin - runs the Jasmin assembler
# 
# Usage:
#     jasmin [-d ]  [ ...]
#

export CLASSPATH=/home/z/Storage/Jasmin/jasmin/classes
echo $CLASSPATH
java jasmin.Main $*