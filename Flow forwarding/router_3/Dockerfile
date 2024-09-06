# Student name: Dmitry Kryukov
FROM openjdk:8
WORKDIR /app/router
# COPY router.jar /compnets/router.jar
COPY . .


#compile
RUN javac -d . *.java
# Install necessary packages 
RUN apt-get update && apt-get install -y tcpdump
RUN jar -cvfe Router.jar Router *.class
#run
# CMD tcpdump -U -w traffic.pcap net 172.18.0.0/16 & java -jar Router.jar
# CMD java -jar Router.jar
CMD tcpdump -i any -U -w traffic_network1.pcap & java -jar Router.jar