FROM balenalib/raspberry-pi:buster

RUN apt-get clean && apt-get update && apt-get upgrade

RUN apt-get -q -y install dnsmasq

ADD ./conf/ /etc

EXPOSE 53/udp

CMD dnsmasq -k