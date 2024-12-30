FROM scratch AS build
ADD build/distributions/mac-app-1.0-SNAPSHOT.tar /app

FROM eclipse-temurin:21.0.5_11-jre AS final

COPY --from=build /app/mac-app-1.0-SNAPSHOT /app