# =================================
# Giai đoạn 1: Build tất cả Module
# =================================
FROM eclipse-temurin:17-jdk-jammy AS builder
WORKDIR /app

# Copy wrapper và POM cha
COPY .mvn/ .mvn
COPY mvnw pom.xml ./

# Copy mã nguồn của tất cả các module
COPY api-gateway/ ./api-gateway/
COPY user-service/ ./user-service/
COPY recipe-service/ ./recipe-service/

# Build toàn bộ dự án MỘT LẦN DUY NHẤT
# (Bỏ qua test để build nhanh hơn)
RUN ./mvnw clean install -DskipTests

# =================================
# Giai đoạn 2: Image cuối cho User Service
# =================================
FROM eclipse-temurin:17-jre-jammy AS user-service-final
WORKDIR /app
# Copy file .jar đã được build từ giai đoạn 'builder'
COPY --from=builder /app/user-service/target/user-service-*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]

# =================================
# Giai đoạn 3: Image cuối cho Recipe Service
# =================================
FROM eclipse-temurin:17-jre-jammy AS recipe-service-final
WORKDIR /app
# Copy file .jar đã được build từ giai đoạn 'builder'
COPY --from=builder /app/recipe-service/target/recipe-service-*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]

# =================================
# Giai đoạn 4: Image cuối cho API Gateway
# =================================
FROM eclipse-temurin:17-jre-jammy AS api-gateway-final
WORKDIR /app
# Copy file .jar đã được build từ giai đoạn 'builder'
COPY --from=builder /app/api-gateway/target/api-gateway-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]