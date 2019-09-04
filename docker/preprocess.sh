#!/bin/bash
set -eu
sed -i'' -e 's/sdfdata_DATA =/sdfdata_DATA :/' Makefile.am
make fullclean &> /dev/null
cp share/strategoxt/strategoxt/strategoxt.jar .
make sdfdata_DATA &> /dev/null
DIR=$(dirname $0)
$DIR/imports.sh
find . -name '*.str' | xargs gawk -i inplace -f $DIR/nullary-constructors.awk
echo "org/webdsl/dsl/syntax/HQL.str org/webdsl/dsl/syntax/WebDSL.str org/webdsl/dsl/syntax/mobl/MoBL.str"
