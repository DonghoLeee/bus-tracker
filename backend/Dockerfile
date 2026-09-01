# 1. 실행 환경 (JRE 17 경량 이미지)
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# GitHub Actions에서 빌드된 jar 파일을 복사
COPY build/libs/*-SNAPSHOT.jar app.jar
# (만약 SNAPSHOT이 아니라면 COPY build/libs/*.jar app.jar)

# e2-micro 메모리 보호를 위해 최대 힙 256MB 제한
ENV JAVA_OPTS="-Xms128m -Xmx256m -Duser.timezone=Asia/Seoul"

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
