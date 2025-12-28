FROM maven:3-jdk-8 as compiler

COPY . /web-ui

RUN cd /web-ui/dependencies && \
    . ./mvn_local_installs && \
    cd /web-ui && \
    mvn package -Dmaven.test.skip=true && \
    mv target/sync-endpoint-web-ui-*.jar /sync-endpoint-web-ui.jar

FROM amazoncorretto:8

# Control Java heap and metaspace sizes
ENV MIN_HEAP 256m
ENV MAX_HEAP 1024m
ENV MAX_METASPACE 128m
ENV JAVA_OPTS -server -Xms$MIN_HEAP -Xmx$MAX_HEAP -XX:MaxMetaspaceSize=$MAX_METASPACE -XX:+UseG1GC

ENV SPRING_CONFIG_LOCATION file:/org.opendatakit.sync-web-ui.application.properties

COPY --from=compiler /sync-endpoint-web-ui.jar /sync-endpoint-web-ui.jar

ENTRYPOINT [ "sh", "-c", "java $JAVA_OPTS -jar /sync-endpoint-web-ui.jar" ]

EXPOSE 8080
