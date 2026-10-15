@echo off
rem ============================================================================
rem  Chay backend (REST API + phuc vu giao dien PWA) tai http://localhost:8090
rem  Can JDK 21 tro len. Lan dau Maven tai thu vien mat 1-3 phut.
rem    run.cmd          chay
rem    run.cmd test     chay kiem thu tu dong (mvnw test)
rem  Khoa AI (tuy chon): set GEMINI_API_KEY=... hoac set ANTHROPIC_API_KEY=... va AI_PROVIDER=claude
rem ============================================================================
setlocal
cd /d "%~dp0"

rem Uu tien JDK 21 neu co (JDK moi hon van chay duoc vi pom da khai bao Lombok)
if exist "C:\Program Files\Java\jdk-21.0.11\bin\java.exe" set "JAVA_HOME=C:\Program Files\Java\jdk-21.0.11"

if /i "%~1"=="test" (
  call "%~dp0mvnw.cmd" -B test
) else (
  call "%~dp0mvnw.cmd" -B spring-boot:run
)
endlocal
