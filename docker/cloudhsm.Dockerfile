# Use the amazon linux image
FROM amazonlinux:2

# Install CloudHSM client
RUN yum install -y https://s3.amazonaws.com/cloudhsmv2-software/CloudHsmClient/EL7/cloudhsm-client-latest.el7.x86_64.rpm

# Install CloudHSM Java library
RUN yum install -y https://s3.amazonaws.com/cloudhsmv2-software/CloudHsmClient/EL7/cloudhsm-client-jce-latest.el7.x86_64.rpm

# Install Java, Maven, wget, unzip and ncurses-compat-libs
RUN yum install -y java wget unzip ncurses-compat-libs
RUN yum install -y which

COPY cloudhsm-entrypoint.sh /opt/ethsigner/bin/cloudhsm-entrypoint.sh
RUN chmod +x /opt/ethsigner/bin/cloudhsm-entrypoint.sh

COPY ethsigner /opt/ethsigner/
WORKDIR /opt/ethsigner

# Expose services ports
# 8545 HTTP JSON-RPC
EXPOSE 8545

ENTRYPOINT ["/opt/ethsigner/bin/cloudhsm-entrypoint.sh"]

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
