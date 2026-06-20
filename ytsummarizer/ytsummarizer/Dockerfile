FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

RUN apt-get update && apt-get install -y \
    ffmpeg \
    python3 \
    python3-pip \
    curl \
    unzip \
    && rm -rf /var/lib/apt/lists/*

RUN pip3 install youtube-transcript-api requests

RUN ln -s /usr/local/bin/youtube_transcript_api /usr/bin/youtube_transcript_api

RUN curl -L https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp -o /usr/local/bin/yt-dlp \
    && chmod a+rx /usr/local/bin/yt-dlp

RUN curl -fsSL https://deno.land/install.sh | DENO_INSTALL=/usr/local sh \
    && ln -s /usr/local/bin/deno /usr/bin/deno
ENV DENO_INSTALL=/usr/local

RUN pip3 install yt-dlp-invidious

COPY --from=build /app/target/*.jar app.jar

RUN useradd -m -u 10014 appuser
RUN chown -R appuser:appuser /app
USER 10014

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]