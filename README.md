# Benchmarking setup for Stratego's incremental compiler

This benchmarking setup is based on Docker so an image with which to benchmark can be easily distributed. 

## Building the image

* First build the [docker image for nightly Spoofax][0]. 
* Then cd into `docker` in this repo. 
* Use `make build` to build the image.

## Running the benchmark

* Use `make run` to run the image. 
* Within the image cd into `~/webdsl/src`.
* Then run `~/run-webdsl-benchmark.sh`. 
* The resulting CSV will be put in `~/bench.csv`.
* You can generate the bar chart by going to `~/stratego-sep-comp-benchmmark/barchartgen` and running `./gen.sh`.
* The resulting PDF will be put next to the csv: `~/bench.pdf`. 
* `~/stratego-sep-comp-benchmmark/boxplot.py` can be used on the `~/bench.csv` to create a single boxplot out of all measurements instead.

Of course you can also just look into the scripts to see how the benchmark is run with these defaults and adapt however you like. 

[0]: https://gitlab.ewi.tudelft.nl/spoofax/docker-images/tree/master/spoofax-build
