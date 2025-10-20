# Stage 1: Build ứng dụng
FROM eclipse-temurin:17-jdk-jammy AS builder
WORKDIR /app

# Copy wrapper và pom GỐC (ĐÃ XÓA ../)
COPY .mvn/ .mvn
COPY mvnw pom.xml ./

# Copy mã nguồn của TẤT CẢ các module (ĐÃ XÓA ../)
COPY api-gateway/ ./api-gateway/
COPY user-service/ ./user-service/
COPY recipe-service/ ./recipe-service/

# Build toàn bộ dự án
RUN ./mvnw clean install -DskipTests

# Stage 2: Tạo image chạy
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Copy CHỈ file .jar của service này
COPY --from=builder /app/recipe-service/target/recipe-service-*.jar app.jar

EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]