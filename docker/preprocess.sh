#!/bin/sh
sed -i'' -e 's/sdfdata_DATA =/sdfdata_DATA :/' Makefile.am && make sdfdata_DATA
$(dirname $0)/imports.sh
find . -name '*.str' | xargs gawk -i inplace -f $(dirname $0)/nullary-constructors.awk