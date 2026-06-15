# ========================================================
# STEP 1: Build the Spring Boot JAR using Maven
# ========================================================
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# Dependencies fetch karne ke liye files copy karo
COPY pom.xml .
COPY src ./src

# Final executable build karo bina tests run kiye
RUN mvn clean package -DskipTests

# ========================================================
# STEP 2: Create final execution image with runtime binaries
# ========================================================
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Install FFmpeg, Python3, and python3-pip
RUN apt-get update && apt-get install -y \
    ffmpeg \
    python3 \
    python3-pip \
    curl \
    && rm -rf /var/lib/apt/lists/*

# 🚀 FIX: Flag hata diya, ab normal install hoga smoothly
RUN pip3 install youtube-transcript-api

# Symlink create karo taaki path instant lock ho jaye
RUN ln -s /usr/local/bin/youtube_transcript_api /usr/bin/youtube_transcript_api

# Install the latest version of yt-dlp binary directly
RUN curl -L https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp -o /usr/local/bin/yt-dlp \
    && chmod a+rx /usr/local/bin/yt-dlp

# Copy the built jar from the build stage
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]