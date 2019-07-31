# Adopt OpenJDK
FROM spoofax/spoofax-build:latest
USER root



# Metadata
ARG BUILD_DATE
ARG VCS_REF
LABEL maintainer="Jeff Smits <j.smits-1@tudelft.nl>"
LABEL org.label-schema.schema-version="1.0"
LABEL org.label-schema.build-date=$BUILD_DATE
LABEL org.label-schema.name="Spoofax Environment for Benchmarking Stratego Separate Compilation"
LABEL org.label-schema.description="An environment for benchmarking Stratego Separate Compilation."
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
  && cd ../..

# Run
CMD [ "/bin/bash" ]
