#!/bin/sh
set -euv
sed -i'' -e 's/sdfdata_DATA =/sdfdata_DATA :/' Makefile.am
make sdfdata_DATA 2>&1
DIR=$(dirname $0)
$DIR/imports.sh
find . -name '*.str' | xargs gawk -i inplace -f $DIR/nullary-constructors.awk