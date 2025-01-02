FROM scratch AS build
ADD build/distributions/mac-app-1.0-SNAPSHOT.tar /app

FROM eclipse-temurin:21.0.5_11-jre AS final

COPY --from=build /app/mac-app-1.0-SNAPSHOT /app
ENV MAC_IN_DOCKER_CONTAINER=true
ENV MAC_BASE_DIR=/app

ENTRYPOINT /app/bin/mac-app
WORKDIR /app