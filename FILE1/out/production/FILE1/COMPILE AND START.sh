#!/bin/bash

cd "$(dirname "$0")"

echo "Compiling BioServer source files..."

mkdir -p bin
javac -cp "lib/gson-2.10.1.jar" -d bin bioserver/*.java

if [ $? -eq 0 ]; then
    echo "Starting BioServer..."
    java -cp "lib/gson-2.10.1.jar:bin" bioserver.ServerMain
else
    echo "Compilation failed. Aborting."
    exit 1
fi

