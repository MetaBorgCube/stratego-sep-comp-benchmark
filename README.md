# Benchmarking setup for Stratego's incremental compiler

This benchmarking setup is based on Docker so an image with which to benchmark can be easily distributed. 

## Building the image

* First build the [docker image for nightly Spoofax][0]. 
* Then cd into `docker` in this repo. 
* Use `make build` to build the image.

## Running the benchmark

* Use `make run` to run the image. 
* Within the image cd into `~/webdsl/src`.
* Then run `~/run-webdsl-benchmark.sh`, the resulting CSV will be put in `~/bench.csv`. Be sure to save any previously generated results before running a new benchmark!
* You can generate the bar chart by going to `~/barchartgen.py ~/bench.csv ~/bench.pdf`.
* `~/boxplot.py ~/bench.csv ~/bench.pdf` can be used to create a single boxplot out of all measurements instead.
* The boxplot script is usually run on the results of the benchmark with the original compiler (`~/run-webdsl-benchmark-batch.sh`). 

Of course you can also just look into the scripts to see how the benchmark is run with these defaults and adapt however you like. 

[0]: https://gitlab.ewi.tudelft.nl/spoofax/docker-images/tree/master/spoofax-build
