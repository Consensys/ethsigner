
FROM openjdk:11.0.8-jre-slim-buster

COPY ethsigner /opt/ethsigner/
WORKDIR /opt/ethsigner

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
      org.label-schema.url="https://docs.ethsigner.pegasys.tech/" \
      org.label-schema.vcs-ref=$VCS_REF \
      org.label-schema.vcs-url="https://github.com/PegaSysEng/ethsigner.git" \
      org.label-schema.vendor="Pegasys" \
      org.label-schema.version=$VERSION \
      org.label-schema.schema-version="1.0"