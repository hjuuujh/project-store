FROM openjdk:11-jdk

COPY ./store-reservation /store-reservation
WORKDIR /store-reservation

CMD ["./gradlew", "bootRun"]