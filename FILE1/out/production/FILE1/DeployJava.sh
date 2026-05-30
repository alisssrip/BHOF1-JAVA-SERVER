#!/bin/bash
set -euo pipefail

cd "$(dirname "$0")"

REMOTE_DIR="/home/alicia/OutbreakServer"

echo "Compiling..."
rm -rf bin
mkdir -p bin
javac --release 17 -cp "lib/gson-2.10.1.jar" -d bin bioserver/*.java

echo "Deploying to naoto..."
rsync -avz --delete --no-o --no-g \
    bin/ \
    "naoto:${REMOTE_DIR}/bin/"

rsync -avz --no-o --no-g \
    lib/ \
    "naoto:${REMOTE_DIR}/lib/"

echo "Restarting service..."
ssh -t naoto "sudo systemctl restart OutbreakServer.service && sudo systemctl status OutbreakServer.service --no-pager -l"

echo "Done."
