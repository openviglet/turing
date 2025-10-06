@ECHO OFF
call mvn clean package -P javascript-compile
call mvn clean install