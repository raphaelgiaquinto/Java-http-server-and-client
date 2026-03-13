FROM openjdk:25-ea-jdk

WORKDIR /app

COPY HTTPServer.java .

RUN javac --enable-preview --release 25 HTTPServer.java

EXPOSE 8888

CMD ["java", "HTTPServer"]