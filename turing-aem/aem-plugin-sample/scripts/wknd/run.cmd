@ECHO OFF
call %~dp0env.cmd
java -Dloader.path=%~dp0libs ^
-Dturing.url=%TURING_URL% ^
-Dturing.apiKey=%TURING_API_KEY% ^
-Dspring.h2.console.enabled=true ^
-jar %~dp0turing-connector.jar