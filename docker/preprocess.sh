#!/bin/sh
$(dirname $0)/imports.sh
find . -name '*.str' | xargs gawk -i inplace -f $(dirname $0)/nullary-constructors.awk