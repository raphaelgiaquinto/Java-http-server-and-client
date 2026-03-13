FROM openjdk:25-ea-jdk

WORKDIR /app

COPY HTTPClient.java .

RUN javac --enable-preview --release 25 HTTPClient.java

ENTRYPOINT ["java", "HTTPClient"]