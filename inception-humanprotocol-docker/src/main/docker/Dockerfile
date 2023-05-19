############################################################
#
# Dockerfile to build INCEpTION container images
#
############################################################

FROM openjdk:21-ea-21-jdk-slim

MAINTAINER INCEpTION Team

# make sure INCEpTION is running in en_US.UTF-8 locale
RUN set -ex \
      && DEBIAN_FRONTEND=noninteractive \
      && apt-get update \
      && apt-get install -y --no-install-recommends locales
RUN set -ex \
      && sed -i -e 's/# en_US.UTF-8 UTF-8/en_US.UTF-8 UTF-8/' /etc/locale.gen \
      && dpkg-reconfigure --frontend=noninteractive locales \
      && update-locale LANG=en_US.UTF-8
ENV LANG en_US.UTF-8

WORKDIR /opt/inception

COPY @docker.jarfile@ inception-app-standalone.jar

# this will be the INCEpTION home folder
RUN useradd -m inception \
    && mkdir /export \
    && chmod u+Xrw /export \
    && chown -R inception /export
VOLUME /export

EXPOSE 8080

USER inception

ENV JAVA_OPTS="-Xmx750m"

CMD java ${JAVA_OPTS} -XX:+UseContainerSupport -Djava.awt.headless=true -Dinception.home=/export -jar inception-app-standalone.jar
