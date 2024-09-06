# Student name: Dmitry Kryukov
FROM openjdk:8

WORKDIR /app/consumer
# COPY broker.jar /compnets/broker.jar
COPY . .

#compile
RUN javac -d . *.java
#make jar
RUN jar -cvfe Consumer.jar Consumer *.class
#run
CMD java -jar Consumer.jar