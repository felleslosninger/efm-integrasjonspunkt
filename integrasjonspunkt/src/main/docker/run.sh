#!/bin/sh
curl -s http://lb:9099/manage/health && \
    curl -s http://lb:8761/discovery/manage/health  && \
    dockerize wait tcp://logstash:8300 && \
    java -jar ${APP_JAVA_PARAMS} ${APP_DIR}/app.jar ${APP_MAIN_CLASS} --spring.profiles.active=${APP_PROFILE} "$@"
