@ECHO OFF
SETLOCAL ENABLEEXTENSIONS

REM Build Turing JS SDK library
ECHO Building js-sdk-lib...
pushd "%~dp0\turing-js-sdk\js-sdk-lib"
IF EXIST node_modules (
    call npm run build
) ELSE (
    ECHO Installing dependencies for js-sdk-lib...
    call npm install
    call npm run build
)
popd

REM Build JS SDK sample
pushd "%~dp0\turing-js-sdk\js-sdk-sample"
IF EXIST node_modules (
    call npm run compile
) ELSE (
    ECHO Installing dependencies for js-sdk-sample...
    call npm install
    call npm run compile
)
popd

REM Build Java modules
ECHO Building Java modules...
pushd "%~dp0"
call mvn clean install
popd

ECHO All components built successfully.
ENDLOCAL