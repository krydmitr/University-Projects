#Student name: Dmitry Kryukov
FROM openjdk:8

WORKDIR /app/endpoint_1
# COPY broker.jar /compnets/broker.jar
COPY . .

#compile
RUN javac -d . *.java
#make jar
RUN jar -cvfe Endpoint.jar Endpoint *.class
#run
CMD java -jar Endpoint.jar