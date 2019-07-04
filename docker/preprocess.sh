#!/bin/sh
$(dirname $0)/imports.sh
find . -name '*.str' | xargs awk -i inplace -f $(dirname $0)/nullary-constructors.awk