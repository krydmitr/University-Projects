FROM openjdk:8

WORKDIR /app/producer
# COPY broker.jar /compnets/broker.jar
COPY . .

#compile
RUN javac -d . *.java
#make jar
RUN jar -cvfe Producer.jar Producer *.class
#run
CMD java -jar Producer.jar