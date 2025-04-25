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
REM  -XX:+UseParallelGC \          # Включить ParallelGC
REM  -Xms2048m -Xmx8192m \         # Фиксированный размер выделения памяти
REM  -XX:+PrintGCDetails \         # Логировать сборки мусора (для дебага)
REM  -XX:+PrintGCDateStamps \      # Добавить даты в логи GC
REM  -Xloggc:gc.log \              # Сохранять логи GC в файл
REM  -Xms8g -Xmx8g \               # Больше памяти, если нужно
REM  -XX:MaxGCPauseMillis=200 \    # Желаемая максимальная пауза GC (мс)
REM  -XX:GCTimeRatio=99 \          # Цель: 1% времени на GC (99% на работу)
REM  -XX:ParallelGCThreads=4 \     # Количество потоков GC (по умолчанию = кол-во ядер CPU)
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