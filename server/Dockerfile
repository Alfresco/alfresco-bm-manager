# Image provides a container that runs alfresco-benchmark-manager to execute load tests on Alfresco Enterprise Content Services.

# Fetch image based on Java 8
FROM alfresco/alfresco-base-java:8

COPY target/alfresco-bm-manager-${docker.project_version}.war /usr/bin
RUN ln /usr/bin/alfresco-bm-manager-${docker.project_version}.war /usr/bin/alfresco-bm-manager.war

ENV JAVA_OPTS=""
ENTRYPOINT java $JAVA_OPTS -jar /usr/bin/alfresco-bm-manager.war