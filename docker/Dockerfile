FROM openjdk:11-jdk-stretch

RUN sed -i -e 's/# en_US.UTF-8 UTF-8/en_US.UTF-8 UTF-8/' /etc/locale.gen && \
  dpkg-reconfigure --frontend=noninteractive locales && \
  update-locale LANG=en_US.UTF-8

ENV LANG en_US.UTF-8 

# Install Docker from Docker Inc. repositories.
RUN curl -sSL https://get.docker.com/ | sh && apt-get clean

COPY docker-entrypoint.sh /usr/local/bin/
COPY modprobe.sh /usr/local/bin/modprobe

ENTRYPOINT ["docker-entrypoint.sh"]
CMD ["sh"]
