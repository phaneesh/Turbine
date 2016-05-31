FROM ubuntu:14.04

RUN \
  apt-get clean && apt-get update && apt-get install -y --no-install-recommends software-properties-common \
  && add-apt-repository ppa:webupd8team/java \
  && apt-get update \
  && echo debconf shared/accepted-oracle-license-v1-1 select true |  debconf-set-selections \
  && echo debconf shared/accepted-oracle-license-v1-1 seen true |  debconf-set-selections \
  && apt-get install -y --no-install-recommends oracle-java8-installer

EXPOSE 8080

ADD turbine-ext/turbine-discovery-ranger/build/libs/turbine-discovery-ranger-executable*.jar turbine-ranger.jar

CMD sh -c "java -jar -XX:+${GC_ALGO-UseG1GC} -Xms${JAVA_PROCESS_MIN_HEAP-1g} -Xmx${JAVA_PROCESS_MAX_HEAP-1g} turbine-ranger.jar -port 8080 -zookeeper ${ZOOKEEPER} -namespace ${NAMESPACE} -environment ${ENVIRONMENT} -streamPath ${STREAMPATH-hystrix.stream} -services ${SERVICES}"
