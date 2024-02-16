# Utilizar una imagen base con Java 17
FROM openjdk:17-jdk-slim as build

# Instalar Maven
RUN apt-get update && apt-get install -y maven

# Establecer el directorio de trabajo
WORKDIR /app

# Copiar solo el archivo pom.xml
COPY pom.xml /app/pom.xml

# Descargar las dependencias
# Esta capa se almacena en la caché y se reutiliza si pom.xml no cambia
RUN mvn dependency:go-offline

# Ahora copiar el resto de los archivos fuente
COPY src /app/src

# Empaquetar la aplicación
RUN mvn package -DskipTests

# Etapa de ejecución
FROM openjdk:17-jdk-slim
VOLUME /tmp

# Copiar el JAR compilado desde la etapa de construcción
COPY --from=build /app/target/BookGateway-0.0.1-SNAPSHOT.jar /usr/local/lib/BookGateway.jar

# Comando para ejecutar la aplicación, adaptando el puerto a la variable de entorno PORT
CMD java -jar /usr/local/lib/BookGateway.jar --server.port=${PORT:-8081}
