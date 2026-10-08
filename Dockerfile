# Stage 2: Runtime
FROM eclipse-temurin:21-jre
WORKDIR /app

COPY build/libs/cart-service-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java","-jar","app.jar"]

EXPOSE 9090

