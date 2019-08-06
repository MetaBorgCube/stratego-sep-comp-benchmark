#!/bin/sh
set -euv
sed -i'' -e 's/sdfdata_DATA =/sdfdata_DATA :/' Makefile.am
make sdfdata_DATA 2>&1