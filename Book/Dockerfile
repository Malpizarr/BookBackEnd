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
COPY --from=build /app/target/demo-0.0.1-SNAPSHOT.jar /usr/local/lib/demo.jar

# Comando para ejecutar la aplicación, asegurándose de que la variable de entorno PORT se pasa correctamente
CMD java -jar /usr/local/lib/demo.jar --server.port=${PORT:-8080}
