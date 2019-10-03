# Adopt OpenJDK
FROM spoofax/spoofax-build:latest
USER root



# Metadata
ARG BUILD_DATE
ARG VCS_REF
LABEL maintainer="Jeff Smits <j.smits-1@tudelft.nl>"
LABEL org.label-schema.schema-version="1.0"
LABEL org.label-schema.build-date=$BUILD_DATE
LABEL org.label-schema.name="Spoofax Environment for Benchmarking Stratego Incremental Compilation"
LABEL org.label-schema.description="An environment for benchmarking Stratego Incremental Compilation."
LABEL org.label-schema.url="http://spoofax.org/"
LABEL org.label-schema.vcs-url="https://gitlab.ewi.tudelft.nl/spoofax/docker-images"
LABEL org.label-schema.vcs-ref=$VCS_REF
LABEL org.label-schema.vendor="TU Delft PL Group"



# Install packages
RUN apt-get update \
 && apt-get install --no-install-recommends -y \
    make \
    automake \
    autoconf \
    gawk \
 && rm -rf /var/lib/apt/lists/*

COPY run-webdsl-benchmark.sh "~/run-webdsl-benchmark.sh"
COPY run-webdsl-benchmark-batch.sh "~/run-webdsl-benchmark-batch.sh"
RUN mkdir -p "~/preprocess-webdsl"
COPY preprocess.sh "~/preprocess-webdsl/main.sh"
COPY imports.sh "~/preprocess-webdsl/imports.sh"
COPY nullary-constructors.awk "~/preprocess-webdsl/nullary-constructors.awk"
COPY README.md "~/README.md"
COPY ../boxplot.py "~/boxplot.py"
COPY ../barchartgen/gen.py "~/barchart.py"

USER myuser
WORKDIR /home/myuser


RUN pip3 install numpy pandas matplotlib


# Download and build benchmarking tool
RUN git clone https://github.com/MetaBorgCube/stratego-sep-comp-benchmark.git --depth 1 \
  && cd stratego-sep-comp-benchmark \
  && git submodule update --init --remote --recursive --jobs 8 --depth 1 \
  && cd ..

RUN cd stratego-sep-comp-benchmark/stratego.build.bench && mvn -nsu clean install && cd ../..


# Download benchmark inputs
RUN git clone --shallow-since=2016-01-01 https://github.com/webdsl/webdsl.git
RUN cd webdsl \
  && ./bootstrap \
  && ./configure --prefix=/usr/local \
  && cd src \
  && mkdir bin \
  && ./get-stratego-jar.sh \
  && sed -i'' -e 's/sdfdata_DATA =/sdfdata_DATA :/' Makefile.am \
  && make sdfdata_DATA \
  && git checkout -- Makefile.am \
  && cd ../..

# Run
CMD [ "/bin/bash" ]
