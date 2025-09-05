#!/bin/bash

mvn spring-boot:run -pl services/imagestore-service
mvn spring-boot:run -pl services/image-processor-service
mvn spring-boot:run -pl services/ui-service
