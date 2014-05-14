@echo off
setlocal

SET SCRIPT_DIR="%~dp0"
pushd "%~dp0"

set XMS=-Xms64m
set XMX=-Xmx256m

call bin\classpath.cmd

set JAVA_ARGS=%XMX% %XMS% -cp %CP%

set ARGS=

:arg-loop
if "%1" == "" goto continue
set ARGS=%ARGS% %1
shift
goto arg-loop

:continue
popd
java -client %JAVA_ARGS% clojure.main --main zipcodedistance.core %ARGS%

:end
endlocal
