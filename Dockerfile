# syntax=docker/dockerfile:1

# ---------- 1) Build frontend (SvelteKit -> static SPA) ----------
FROM node:22-alpine AS frontend
WORKDIR /fe
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

# ---------- 2) Build backend (bundle SPA in META-INF/resources, poi package) ----------
FROM maven:3.9-eclipse-temurin-21 AS backend
WORKDIR /app
COPY backend/ ./
# Il build statico del frontend viene servito da Quarkus dallo stesso origin
COPY --from=frontend /fe/build/ src/main/resources/META-INF/resources/
RUN mvn -q -B -DskipTests package

# ---------- 3) Runtime (JRE leggero, fast-jar Quarkus) ----------
FROM eclipse-temurin:21-jre
WORKDIR /work
# Ordine ottimale per la cache dei layer: prima le librerie, poi l'app
COPY --from=backend /app/target/quarkus-app/lib/ /work/lib/
COPY --from=backend /app/target/quarkus-app/*.jar /work/
COPY --from=backend /app/target/quarkus-app/app/ /work/app/
COPY --from=backend /app/target/quarkus-app/quarkus/ /work/quarkus/
EXPOSE 8080
ENV QUARKUS_HTTP_HOST=0.0.0.0
# Render assegna la porta via $PORT; in locale/Fly default 8080.
# profilo prod attivo di default sull'artefatto packaged; DB via env (DB_URL/DB_USER/DB_PASSWORD)
CMD java -Dquarkus.http.port=${PORT:-8080} -jar /work/quarkus-run.jar
