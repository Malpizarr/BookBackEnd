# Utilizar una imagen base con Java 17
FROM openjdk:17-jdk-slim as build

# Instalar Maven
RUN apt-get update && apt-get install -y maven

# Establecer el directorio de trabajo
WORKDIR /app

# Copiar los archivos fuente del proyecto
COPY src ./src
COPY pom.xml .

# Empaquetar la aplicación
RUN mvn clean package -DskipTests

# Etapa de ejecución
FROM openjdk:17-jdk-slim
VOLUME /tmp

# Copiar el JAR compilado desde la etapa de construcción
COPY --from=build /app/target/BookFriendship-0.0.1-SNAPSHOT.jar /usr/local/lib/BookFriendship.jar

# Exponer el puerto en el que corre la aplicación
EXPOSE 8082

# Comando para ejecutar la aplicación, permitiendo la configuración del puerto mediante una variable de entorno
CMD java -jar /usr/local/lib/BookFriendship.jar --server.port=${PORT:-8082}
