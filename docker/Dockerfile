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



USER myuser
WORKDIR /home/myuser



# Download and build benchmarking tool
RUN git clone https://github.com/MetaBorgCube/stratego-sep-comp-benchmark.git --depth 1 \
  && cd stratego-sep-comp-benchmark \
  && git submodule update --init --remote --recursive --jobs 8 --depth 1 \
  && cd ..

RUN cd stratego-sep-comp-benchmark && ./build.sh && cd ..


# Download benchmark inputs
RUN git clone https://github.com/webdsl/webdsl.git \
  && cd webdsl \
  && ./bootstrap \
  && ./configure --prefix=/usr/local \
  && cd src \
  && ./get-stratego-jar.sh \
  && sed -i '' -e 's/sdfdata_DATA =/sdfdata_DATA :/' Makefile.am \
  && make sdfdata_DATA \
  && git checkout -- Makefile.am \
  && cd ../..

# Run
CMD [ "/bin/bash" ]
