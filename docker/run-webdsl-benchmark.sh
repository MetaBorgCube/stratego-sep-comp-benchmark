java -Xms512m -Xmx2048m -Xss16m -jar ../../stratego-sep-comp-benchmark/stratego.build.bench/target/stratego.build.bench.jar benchmark stratego --git-dir .. --project-dir . --main-file webdslc.str --package-name org.webdsl.webdslc --preprocess ../../stratego-sep-comp-benchmark/docker/preprocess.sh --start-commit-hash 033e6ad9e48a78454b85e3ce8cd7b94d35cb09e3 --end-commit-hash aaf8241d8c0ce5e91c5bdba1aa0fdfbfa7e78a5b -I . -I org/webdsl/dsl/syntax -I share/strategoxt/java_front/languages/stratego-java --main-strategy webdslc-main --output-file src-gen/org/webdsl/webdslc/Main.java --class-path "strategoxt.jar:src-gen:." --output-dir bin -DPACKAGE_VERSION_TERM="BENCH"