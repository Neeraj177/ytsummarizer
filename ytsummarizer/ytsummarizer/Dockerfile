# ========================================================
# STEP 1: Build the Spring Boot JAR using Maven
# ========================================================
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn clean package -DskipTests

# ========================================================
# STEP 2: Create final execution image with runtime binaries
# ========================================================
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

RUN apt-get update && apt-get install -y \
    ffmpeg \
    python3 \
    python3-pip \
    curl \
    unzip \
    && rm -rf /var/lib/apt/lists/*

RUN pip3 install youtube-transcript-api

RUN ln -s /usr/local/bin/youtube_transcript_api /usr/bin/youtube_transcript_api

RUN curl -L https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp -o /usr/local/bin/yt-dlp \
    && chmod a+rx /usr/local/bin/yt-dlp

# Deno install karo yt-dlp JS runtime ke liye
RUN curl -fsSL https://deno.land/install.sh | DENO_INSTALL=/usr/local sh \
    && ln -s /usr/local/bin/deno /usr/bin/deno
ENV DENO_INSTALL=/usr/local

# ✅ Invidious fallback plugin install karo
RUN pip3 install yt-dlp-invidious

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]