# 后端镜像：多阶段构建（Maven 编译 → JRE 运行）
# 容器内使用 backend/maven-settings.xml（阿里云镜像），避免直连 Maven Central 超时

# ---------- 构建阶段 ----------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY . .
RUN mvn -s maven-settings.xml -B -q -DskipTests package

# ---------- 运行阶段 ----------
FROM eclipse-temurin:17-jre
WORKDIR /app

# 时区 + 健康检查所需 curl
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl tzdata \
    && rm -rf /var/lib/apt/lists/* \
    && ln -snf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime \
    && echo "Asia/Shanghai" > /etc/timezone

COPY --from=build /build/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
