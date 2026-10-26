FROM gcr.io/distroless/java25-debian13@sha256:9edcd1e8d3ee33c296ec4a2318500c5b0824f953e27007c36dfbb50b7205d7da

ENV JDK_JAVA_OPTIONS="-XX:MaxRAMPercentage=65.0 -XX:+UseParallelGC -XX:ActiveProcessorCount=2 -XX:+UseCompactObjectHeaders"

COPY build/libs/app.jar /app/
WORKDIR /app
CMD ["app.jar"]

