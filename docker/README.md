# Benchmarking setup for Stratego's incremental compiler

This docker image should be able to run our WebDSL benchmark without external resources. Everything is already downloaded, built, and packaged. 

## Running a benchmark

From the home directory `~`, run `~/run-webdsl-benchmark.sh` for the benchmark with Stratego's new incremental compiler. The results will end up in `~/bench.csv`. The output shows the progress of the benchmark. These prints happen in between measurements where we also do garbage collection, switching the git history of the WebDSL codebase to the next commit, and the preprocessing before the next measurement. It shouldn't influence the results of the benchmark. 

From the home directory, run `~/run-webdsl-benchmark-batch.sh` for the benchmark with Stratego's original compiler. Again, the results will end up in `~/bench.csv`, so make sure you rename any earlier results. 

## Producing a visualisation

To produce the stacked barchart, use `~/barchart.py ~/bench.csv ~/bench.pdf`. 

To produce the boxplot of the measurements with the original compiler, use the `~/boxplot.py ~/bench.csv ~/bench.pdf`.
