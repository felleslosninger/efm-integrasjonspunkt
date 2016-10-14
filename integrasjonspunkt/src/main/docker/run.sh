#!/bin/sh

dockerize -wait http://lb:9099 -wait http://lb:8761 -wait tcp://logstash:8300

java -jar ${APP_JAVA_PARAMS} ${APP_DIR}/app.jar ${APP_MAIN_CLASS} --spring.profiles.active=${APP_PROFILE} "$@"
