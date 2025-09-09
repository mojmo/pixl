FROM maven:sapmachine AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

FROM tomcat:11-jre21
COPY --from=build /app/target/ROOT.war /usr/local/tomcat/webapps/ROOT.war
EXPOSE 8080
