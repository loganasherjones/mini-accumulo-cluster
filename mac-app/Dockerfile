FROM scratch AS build
ARG PROJECT_VERSION
ADD build/distributions/mac-app-$PROJECT_VERSION.tar /app

FROM eclipse-temurin:21.0.5_11-jre AS final
ARG PROJECT_VERSION

COPY --from=build /app/mac-app-${PROJECT_VERSION} /app
ENV MAC_IN_DOCKER_CONTAINER=true
ENV MAC_BASE_DIR=/app

ENTRYPOINT /app/bin/mac-app
WORKDIR /app