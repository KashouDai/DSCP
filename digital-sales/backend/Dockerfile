FROM maven:3.9.10-eclipse-temurin-17 AS build
WORKDIR /build
COPY . .
RUN mvn -Dmaven.repo.local=/root/.m2/repository clean package -pl dop-admin -am -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /build/dop-admin/target/dop-admin.jar /app/app.jar
RUN apt-get update \
    && apt-get install -y --no-install-recommends wget \
    && rm -rf /var/lib/apt/lists/* \
    && mkdir -p /app/data/uploadPath /app/logs
EXPOSE 8080
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-Duser.timezone=Asia/Shanghai", "-jar", "/app/app.jar"]
