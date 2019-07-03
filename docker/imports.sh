#!/bin/sh
find . -name '*.str' | xargs sed -i '' -e '
s:libwebdsl-generator:org/webdsl/dsl/generation/webdsl-generator:
s:libwebdsl-front:org/webdsl/dsl/syntax/webdsl-front:
s:libto-java-servlet:org/webdsl/dsl/to-java-servlet/to-java-servlet:
s:libwrite-files:org/webdsl/dsl/write-files/write-files:
s:libjava-transformations:org/webdsl/dsl/java-transformations/java-transformations:
s:libback-end-transformations:org/webdsl/dsl/back-end-transformations/back-end-transformations:'