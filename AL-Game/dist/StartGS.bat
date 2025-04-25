@echo off
TITLE Aion 5.8 - Game Emu Console
@COLOR 4B
REM -------------------------------------
REM Указываем свой путь к JDK8
SET PATH="E:\Java\jdk1.8.0_321\bin"

:START
CLS

echo.
echo Starting Aion Version 5.8
echo.

REM -------------------------------------
REM Оптимальные параметры для ParallelGC
REM  -XX:+UseParallelGC \          # Enable ParallelGC
REM  -Xms2048m -Xmx8192m \         # Fixed size of memory allocation
REM  -XX:+PrintGCDetails \         # Log garbage collection (for debug)
REM  -XX:+PrintGCDateStamps \      # Add dates to GC logs
REM  -Xloggc:gc.log \              # Save GC logs to a file
REM  -Xms8g -Xmx8g \               # More memory if needed
REM  -XX:MaxGCPauseMillis=200 \    # Desired maximum GC pause (ms)
REM  -XX:GCTimeRatio=99 \          # Goal: 1% of time for GC (99% for work)
REM  -XX:ParallelGCThreads=4 \     # Number of GC threads (default = number of CPU cores)
REM -------------------------------------

java ^
  -Xms2048m -Xmx8192m ^
  -XX:+UseParallelGC ^
  -XX:+UseParallelOldGC ^
  -XX:ParallelGCThreads=4 ^
  -XX:MaxGCPauseMillis=200 ^
  -XX:GCTimeRatio=99 ^
  -XX:+DisableExplicitGC ^
  -ea ^
  -javaagent:./libs/al-commons.jar ^
  -cp ./libs/*;./libs/AL-Game.jar ^
  com.aionemu.gameserver.GameServer
REM -------------------------------------

if ERRORLEVEL 2 goto restart
if ERRORLEVEL 1 goto error
if ERRORLEVEL 0 goto end

:restart
echo.
echo Administrator Restart ...
echo.
goto start

:error
echo.
echo Server terminated abnormaly ...
echo.
goto end

:end
echo.
echo Server terminated ...
echo.
pause