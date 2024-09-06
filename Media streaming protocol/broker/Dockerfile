# Student name: Dmitry Kryukov
FROM openjdk:8
WORKDIR /app/broker
# COPY broker.jar /compnets/broker.jar
COPY . .


#compile
RUN javac -d . *.java
# Install necessary packages 
RUN apt-get update && apt-get install -y tcpdump
RUN jar -cvfe Broker.jar Broker *.class
#run
CMD tcpdump -U -w traffic.pcap net 172.18.0.0/16 & java -jar Broker.jar