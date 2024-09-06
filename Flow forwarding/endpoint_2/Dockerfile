# Student name: Dmitry Kryukov
FROM openjdk:8

WORKDIR /app/Endpoint_2
# COPY broker.jar /compnets/broker.jar
COPY . .

#compile
RUN javac -d . *.java
#make jar
RUN jar -cvfe Endpoint.jar Endpoint *.class
#run
CMD java -jar Endpoint.jar