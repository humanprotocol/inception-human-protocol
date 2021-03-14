############################################################
#
# Dockerfile to build INCEpTION container images
#
############################################################

FROM openjdk:11

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

#ENV PORT=8080
ENV JAVA_OPTS="-Xmx150m -XX:+UseContainerSupport"

#EXPOSE ${PORT}

USER inception

CMD java ${JAVA_OPTS} -Drecommender.enabled=false -Ddebug.casDoctor.forceReleaseBehavior=true -Dremote-api.enabled=true -Dsearch.enabled=false -Dknowledge-base.enabled=false -Dsharing.invites.enabled=true -Dexternal-search.enabled=false -Ddocumentmetadata.enabled=false -Dui.html.enabled=false -Dui.pdf.enabled=false -Dserver.port=${PORT} -Dserver.tomcat.max-threads=4 -Djava.awt.headless=true -Dinception.home=/export -jar inception-app-standalone.jar