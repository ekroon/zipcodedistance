@echo off
set CP=

for %%i in (%SCRIPT_DIR%\lib\*.jar) do call bin\classpathbuild.cmd %%i
