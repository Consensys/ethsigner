FROM openjdk:11-jdk-stretch

# Install Docker from Docker Inc. repositories.
RUN curl -sSL https://get.docker.com/ | sh && apt-get clean

COPY docker-entrypoint.sh /usr/local/bin/
COPY modprobe.sh /usr/local/bin/modprobe

ENTRYPOINT ["docker-entrypoint.sh"]
CMD ["sh"]
