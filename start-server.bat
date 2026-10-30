@echo off
REM May chu Hu Khong Tu Tien - chi chay server, khong mo game.
REM Lan dau: tu dong dong y EULA va tat xac minh tai khoan (online-mode=false).
cd /d %~dp0
if not exist run\server mkdir run\server
echo eula=true> run\serverula.txt
if not exist run\server\server.properties (
  echo online-mode=false> run\server\server.properties
  echo pvp=true>> run\server\server.properties
  echo motd=Hu Khong Tu Tien>> run\server\server.properties
)
call gradlew.bat runServer
pause
