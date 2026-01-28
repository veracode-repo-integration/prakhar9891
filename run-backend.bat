@echo off
echo ========================================
echo Starting POS Backend Server
echo ========================================
echo.

REM Set environment variables
set MYSQL_URL=jdbc:mysql://localhost:3306/posdb?createDatabaseIfNotExist=true^&useSSL=false^&serverTimezone=UTC^&allowPublicKeyRetrieval=true
set MYSQL_USER=root
set MYSQL_PASSWORD=1234567890
set CLOUDINARY_CLOUD_NAME=demo
set CLOUDINARY_API_KEY=demo
set CLOUDINARY_API_SECRET=demo

echo Environment configured:
echo - MySQL: localhost:3306/posdb
echo - User: root
echo - Cloudinary: demo mode
echo.
echo Starting server on http://localhost:4000
echo Press Ctrl+C to stop
echo.

REM Run with Maven
call mvn spring-boot:run

pause
