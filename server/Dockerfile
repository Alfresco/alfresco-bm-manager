# Image provides a container that runs alfresco-benchmark-manager to execute load tests on Alfresco Enterprise Content Services.

# Fetch image based on Java 8
FROM alfresco/alfresco-base-java:8

COPY target/alfresco-bmf-manager-${docker.project_version}.war /usr/bin
RUN ln /usr/bin/alfresco-bmf-manager-${docker.project_version}.war /usr/bin/alfresco-bmf-manager.war

EXPOSE 9080

ENV JAVA_OPTS=""
ENTRYPOINT java $JAVA_OPTS -jar /usr/bin/alfresco-bmf-manager.war