FROM eclipse-temurin:11 as jre-build

# Create a custom Java runtime
RUN JAVA_TOOL_OPTIONS="-Djdk.lang.Process.launchMechanism=vfork" "$JAVA_HOME/bin/jlink" \
         --add-modules ALL-MODULE-PATH \
         --strip-debug \
         --no-man-pages \
         --no-header-files \
         --compress=2 \
         --output /javaruntime

FROM ubuntu:22.04
ENV JAVA_HOME=/opt/java/openjdk
ENV PATH "${JAVA_HOME}/bin:${PATH}"
COPY --from=jre-build /javaruntime $JAVA_HOME

RUN apt update && apt install -y --no-install-recommends curl iputils-ping net-tools netcat \
 && apt clean \
 && rm -rf /var/lib/apt/lists/*

# Add ethsigner user instead of using root (may bring backward incompatibility for previous directory mounts)
RUN adduser --disabled-password --gecos "" --home /opt/ethsigner ethsigner && \
    chown ethsigner:ethsigner /opt/ethsigner && \
    chmod 755 /opt/ethsigner
USER ethsigner
WORKDIR /opt/ethsigner

COPY --chown=ethsigner:ethsigner ethsigner /opt/ethsigner/

ENV ETHSIGNER_HTTP_LISTEN_HOST="0.0.0.0"
ENV ETHSIGNER_METRICS_HOST="0.0.0.0"

# Expose services ports
# 8545 HTTP JSON-RPC
EXPOSE 8545

ENTRYPOINT ["/opt/ethsigner/bin/ethsigner"]

# Build-time metadata as defined at http://label-schema.org
ARG BUILD_DATE
ARG VCS_REF
ARG VERSION
LABEL org.label-schema.build-date=$BUILD_DATE \
      org.label-schema.name="Ethsigner" \
      org.label-schema.description="Ethereum transaction signing application" \
      org.label-schema.url="https://docs.ethsigner.consensys.net" \
      org.label-schema.vcs-ref=$VCS_REF \
      org.label-schema.vcs-url="https://github.com/ConsenSys/ethsigner" \
      org.label-schema.vendor="Consensys" \
      org.label-schema.version=$VERSION \
      org.label-schema.schema-version="1.0"
