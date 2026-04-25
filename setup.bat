@echo off
echo.
echo ======================================
echo AnimeSense Bot - Setup Script
echo ======================================
echo.

REM Check Java
echo Checking Java version...
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo ERROR: Java is not installed
    echo Please install Java 17 or higher
    pause
    exit /b 1
)
echo Java is installed
echo.

REM Check Maven
echo Checking Maven...
mvn -version >nul 2>&1
if %errorlevel% neq 0 (
    echo ERROR: Maven is not installed
    echo Please install Maven 3.6+
    pause
    exit /b 1
)
echo Maven is installed
echo.

REM Build the project
echo Building the project...
call mvn clean package -DskipTests
if %errorlevel% neq 0 (
    echo ERROR: Build failed
    pause
    exit /b 1
)
echo Build successful!
echo.

echo ======================================
echo Setup complete!
echo ======================================
echo.
echo Next steps:
echo 1. Edit application.properties and add your bot token
echo    Location: src\main\resources\application.properties
echo.
echo 2. Get your bot token from @BotFather on Telegram
echo 3. Get your user ID from @userinfobot on Telegram
echo.
echo 4. Run the bot:
echo    java -jar target\animesense-bot-1.0.0.jar
echo.
echo 5. Access admin panel:
echo    http://localhost:8080/admin/login
echo    Username: admin
echo    Password: admin123
echo.
echo For detailed instructions, see QUICKSTART.md
echo.
pause
