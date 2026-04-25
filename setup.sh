#!/bin/bash

echo "🎬 AnimeSense Bot - Setup Script"
echo "================================"
echo ""

# Check Java version
echo "Checking Java version..."
if ! command -v java &> /dev/null; then
    echo "❌ Error: Java is not installed"
    echo "   Please install Java 17 or higher"
    exit 1
fi

java_version=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
if [ "$java_version" -lt 17 ]; then
    echo "❌ Error: Java 17 or higher is required"
    echo "   Current version: Java $java_version"
    exit 1
fi
echo "✅ Java version: $java_version"
echo ""

# Check Maven
echo "Checking Maven..."
if ! command -v mvn &> /dev/null; then
    echo "❌ Error: Maven is not installed"
    echo "   Please install Maven 3.6+"
    exit 1
fi
echo "✅ Maven is installed"
echo ""

# Build the project
echo "Building the project..."
mvn clean package -DskipTests
if [ $? -eq 0 ]; then
    echo "✅ Build successful"
else
    echo "❌ Build failed"
    exit 1
fi
echo ""

echo "🎉 Setup complete!"
echo ""
echo "Next steps:"
echo "1. Edit application.properties and add your bot token"
echo "   Location: src/main/resources/application.properties"
echo ""
echo "2. Get your bot token from @BotFather on Telegram"
echo "3. Get your user ID from @userinfobot on Telegram"
echo ""
echo "4. Run the bot:"
echo "   java -jar target/animesense-bot-1.0.0.jar"
echo ""
echo "5. Access admin panel:"
echo "   http://localhost:8080/admin/login"
echo "   Username: admin"
echo "   Password: admin123"
echo ""
echo "For detailed instructions, see QUICKSTART.md"
