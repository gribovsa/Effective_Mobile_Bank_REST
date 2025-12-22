# Используем стабильную Java 17 LTS
FROM eclipse-temurin:17-jdk
# Определяем аргумент, проект собирается через Maven, файл будет в target/*.jar
ARG JAR_FILE=target/*.jar
# Копируем JAR
COPY ${JAR_FILE} app.jar
# Настройка часового пояса и запуск
ENV TZ=Europe/Moscow
ENTRYPOINT ["java", "-jar", "/app.jar"]