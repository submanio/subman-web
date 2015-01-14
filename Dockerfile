FROM ubuntu:14.04
MAINTAINER Vladimir Iakovlev <nvbn.rm@gmail.com>

RUN adduser --disabled-password --gecos "" subman

ENV "VERSION" 2015_01_12_02_16

RUN apt-get update -yqq
RUN apt-get upgrade -yqq
RUN apt-get install software-properties-common python-software-properties -yqq --no-install-recommends
RUN add-apt-repository ppa:chris-lea/node.js  -y
RUN apt-get update -yqq
RUN apt-get install nodejs ruby openjdk-7-jdk curl git -yqq --no-install-recommends
RUN npm install -g bower
RUN gem install sass
RUN curl -s https://raw.githubusercontent.com/technomancy/leiningen/2.5.0/bin/lein > /usr/local/bin/lein
RUN chmod 0755 /usr/local/bin/lein

WORKDIR /home/subman
COPY . /home/subman/code
RUN chown -R subman code
USER subman
WORKDIR /home/subman/code

RUN lein bower install
RUN sass resources/public/main.sass > resources/public/main.css

RUN lein ring uberjar >> /dev/null 2>> /dev/null

CMD java -jar target/subman-web-*-SNAPSHOT-standalone.jar
