# 🎬 AnimeSense - Java Edition

**Intelligent Anime Recommendation Bot with Web Admin Panel**

A complete Spring Boot application featuring:
- 🤖 Telegram bot for users to get personalized anime recommendations
- 🌐 Web-based admin panel for content and user management
- 🧠 Smart taste-based recommendation engine
- 📊 Real-time statistics and analytics
- 📢 Broadcast messaging system

![Java](https://img.shields.io/badge/Java-17+-orange.svg)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.0-green.svg)
![License](https://img.shields.io/badge/license-MIT-blue.svg)

---

## 📋 Table of Contents

- [Features](#features)
- [Architecture](#architecture)
- [Quick Start](#quick-start)
- [Configuration](#configuration)
- [Admin Panel](#admin-panel)
- [Telegram Bot](#telegram-bot)
- [Database](#database)
- [API Integration](#api-integration)
- [Deployment](#deployment)
- [Troubleshooting](#troubleshooting)

---

## ✨ Features

### 🤖 Telegram Bot Features
- **Smart Recommendations**: AI-powered suggestions based on user taste profile
- **Multiple Recommendation Modes**:
  - 🧠 Smart: Personalized based on favorites and ratings
  - 🔥 Trending: Currently popular anime
  - 🎲 Random: Discover new genres
- **Favorites Management**: Track anime with status (watching/completed/plan to watch/dropped)
- **Rating System**: Rate anime 1-10 to improve recommendations
- **Search Function**: Find any anime from MyAnimeList
- **User Statistics**: View personal stats and top genres

### 🌐 Web Admin Panel Features
- **Dashboard**: Real-time statistics and user activity
- **User Management**: View all users, activity, and favorites
- **Anime Management**:
  - Add anime from MyAnimeList by ID
  - Set featured anime (1.5x recommendation boost)
  - Adjust admin boost (0.1-5.0x multiplier)
  - Delete anime from database
- **Broadcast System**: Send announcements to all users
- **Secure Authentication**: Login-protected admin area

### 🧠 Recommendation Engine
- **Taste Profiling**: Learns from user interactions
- **Genre Weighting**: Adjusts based on ratings
  - High ratings (7+) → Increase genre weight
  - Low ratings (≤4) → Decrease genre weight
- **Smart Scoring Algorithm**:
  ```
  Score = (Base Rating × 10 + Genre Bonuses) × Admin Boost × Featured Bonus
  ```
- **Filtering**: Excludes already favorited anime and low-rated content

---

## 🏗️ Architecture

```
animesense-java/
├── src/main/java/com/animesense/
│   ├── AnimeSenseApplication.java    # Main Spring Boot application
│   ├── bot/
│   │   └── AnimeSenseBot.java        # Telegram bot implementation
│   ├── controller/
│   │   └── AdminController.java      # Web admin panel endpoints
│   ├── model/                        # JPA entities
│   │   ├── User.java
│   │   ├── Anime.java
│   │   ├── Favorite.java
│   │   ├── TasteProfile.java
│   │   └── BroadcastMessage.java
│   ├── repository/                   # Spring Data JPA repositories
│   ├── service/                      # Business logic
│   │   ├── UserService.java
│   │   ├── AnimeService.java
│   │   ├── FavoriteService.java
│   │   ├── TasteProfileService.java
│   │   ├── RecommendationService.java
│   │   ├── JikanApiService.java
│   │   └── BroadcastService.java
│   ├── config/
│   │   └── SecurityConfig.java       # Spring Security configuration
│   └── dto/                          # Data Transfer Objects
├── src/main/resources/
│   ├── application.properties        # Configuration
│   ├── templates/admin/              # Thymeleaf templates
│   └── static/css/                   # Admin panel CSS
└── pom.xml                           # Maven dependencies
```

### Technology Stack
- **Framework**: Spring Boot 3.2.0
- **Language**: Java 17+
- **Database**: H2 (development) / MySQL (production)
- **ORM**: Spring Data JPA / Hibernate
- **Telegram**: TelegramBots 6.9.7
- **View**: Thymeleaf
- **Security**: Spring Security
- **API Client**: OkHttp
- **Build Tool**: Maven

---

## 🚀 Quick Start

### Prerequisites
- Java 17 or higher
- Maven 3.6+
- Telegram Bot Token (from [@BotFather](https://t.me/botfather))
- Your Telegram User ID (from [@userinfobot](https://t.me/userinfobot))

### Installation

**1. Get Your Bot Token**
```
1. Open Telegram and search for @BotFather
2. Send /newbot
3. Follow prompts to create your bot
4. Copy the bot token
```

**2. Get Your User ID**
```
1. Search for @userinfobot
2. Send /start
3. Copy your user ID
```

**3. Configure Application**

Edit `src/main/resources/application.properties`:

```properties
# Telegram Bot
telegram.bot.token=YOUR_BOT_TOKEN_HERE
telegram.bot.username=your_bot_username

# Admin Settings
admin.username=admin
admin.password=admin123
admin.telegram.ids=YOUR_TELEGRAM_USER_ID

# Database (H2 for development)
spring.datasource.url=jdbc:h2:file:./data/animesense
```

**4. Build and Run**

```bash
# Build the project
mvn clean package

# Run the application
java -jar target/animesense-bot-1.0.0.jar

# Or run with Maven
mvn spring-boot:run
```

**5. Access Admin Panel**

Open your browser and navigate to:
```
http://localhost:8080/admin/login

Username: admin
Password: admin123
```

**6. Test the Bot**

Open Telegram, search for your bot, and send `/start`

---

## ⚙️ Configuration

### application.properties Options

```properties
# Server
server.port=8080

# Telegram Bot
telegram.bot.token=YOUR_BOT_TOKEN
telegram.bot.username=your_bot_username

# Admin Account
admin.username=admin
admin.password=changeme123
admin.telegram.ids=123456789,987654321  # Comma-separated

# Database - H2 (Development)
spring.datasource.url=jdbc:h2:file:./data/animesense
spring.datasource.username=sa
spring.datasource.password=
spring.h2.console.enabled=true

# Database - MySQL (Production)
# spring.datasource.url=jdbc:mysql://localhost:3306/animesense
# spring.datasource.username=root
# spring.datasource.password=your_password
# spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQL8Dialect

# JPA/Hibernate
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false

# Jikan API
jikan.api.base-url=https://api.jikan.moe/v4
jikan.api.rate-limit-ms=3000

# Recommendations
recommendation.max-per-session=5
recommendation.min-rating=6.0
recommendation.genre-weight-increment=0.1
recommendation.genre-weight-decrement=0.05
```

### Environment Variables

You can also use environment variables:

```bash
export BOT_TOKEN=your_bot_token
export BOT_USERNAME=your_bot_username
export ADMIN_USERNAME=admin
export ADMIN_PASSWORD=secure_password
export ADMIN_TELEGRAM_IDS=123456789
```

---

## 🌐 Admin Panel

### Login
- URL: `http://localhost:8080/admin/login`
- Default credentials: `admin` / `admin123`

### Dashboard
- Total users and active users (7 days / 30 days)
- Total anime in database
- Recent users list
- Featured anime

### User Management
- View all registered users
- See user activity and last active time
- View user's favorite anime and ratings
- Monitor user statistics

### Anime Management

**Add Anime:**
1. Get MyAnimeList ID from URL (e.g., `https://myanimelist.net/anime/52991/...` → ID is `52991`)
2. Go to Anime → Add Anime
3. Enter the ID and click "Add Anime"
4. System fetches data from MyAnimeList automatically

**Edit Anime:**
1. Go to Anime → Find anime → Edit
2. Toggle "Featured" (adds 1.5x boost)
3. Set "Admin Boost" (0.1-5.0x multiplier)
4. Save changes

**Example Boost Calculation:**
- Base boost: 2.0x
- Featured: Yes (1.5x)
- **Total: 3.0x** (anime is 3x more likely to be recommended)

### Broadcast
- Send announcements to all users
- View broadcast history
- Track sent/failed counts

---

## 🤖 Telegram Bot

### User Commands
- `/start` - Initialize bot and show menu
- `/help` - Display help information

### Menu Buttons
- **🎬 Get Recommendation** - Start recommendation flow
- **⭐ My Favorites** - View your anime collection
- **🔍 Search Anime** - Find specific anime
- **📊 My Stats** - View your statistics
- **❓ Help** - Get help

### Recommendation Flow
1. User clicks "Get Recommendation"
2. Selects recommendation type (Smart/Trending/Random)
3. Bot shows anime with:
   - Title and image
   - Rating and genres
   - Synopsis
4. User can:
   - Add to favorites → Select status → Rate 1-10
   - Skip to next recommendation

### Status Types
- 📺 **Watching** - Currently watching
- ✅ **Completed** - Finished watching
- 📋 **Plan to Watch** - Want to watch later
- ❌ **Dropped** - Stopped watching

---

## 💾 Database

### H2 Database (Development)

Built-in H2 console is enabled by default:
- URL: `http://localhost:8080/h2-console`
- JDBC URL: `jdbc:h2:file:./data/animesense`
- Username: `sa`
- Password: (leave empty)

### MySQL Setup (Production)

**1. Create Database**
```sql
CREATE DATABASE animesense CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'animesense'@'localhost' IDENTIFIED BY 'your_password';
GRANT ALL PRIVILEGES ON animesense.* TO 'animesense'@'localhost';
FLUSH PRIVILEGES;
```

**2. Update application.properties**
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/animesense
spring.datasource.username=animesense
spring.datasource.password=your_password
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQL8Dialect
```

### Database Schema

**Tables:**
- `users` - User accounts and admin flags
- `anime` - Anime information from MyAnimeList
- `favorites` - User-anime relationships with ratings
- `taste_profiles` - Genre preferences per user
- `broadcast_messages` - Broadcast history

---

## 🔌 API Integration

### Jikan API (MyAnimeList)

AnimeSense uses the Jikan API to fetch anime data:

**Base URL:** `https://api.jikan.moe/v4`

**Rate Limiting:**
- Default: 3 seconds between requests
- Automatic rate limiting in `JikanApiService`

**Endpoints Used:**
- `GET /anime/{id}` - Get anime by ID
- `GET /anime?q={query}` - Search anime
- `GET /top/anime` - Get top anime
- `GET /anime?genres={ids}` - Get anime by genres

---

## 🚀 Deployment

### Option 1: Standalone JAR

```bash
# Build
mvn clean package

# Run
java -jar target/animesense-bot-1.0.0.jar
```

### Option 2: Systemd Service (Linux)

**1. Create service file:** `/etc/systemd/system/animesense.service`

```ini
[Unit]
Description=AnimeSense Bot
After=network.target

[Service]
Type=simple
User=your_username
WorkingDirectory=/path/to/animesense-java
ExecStart=/usr/bin/java -jar /path/to/animesense-java/target/animesense-bot-1.0.0.jar
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

**2. Enable and start**
```bash
sudo systemctl daemon-reload
sudo systemctl enable animesense
sudo systemctl start animesense
sudo systemctl status animesense
```

**3. View logs**
```bash
sudo journalctl -u animesense -f
```

### Option 3: Docker (Coming Soon)

---

## 🐛 Troubleshooting

### Bot doesn't start
**Problem:** Application fails to start

**Solutions:**
- Check bot token is correct in `application.properties`
- Verify Java version: `java -version` (need 17+)
- Check logs for error messages

### Bot doesn't respond
**Problem:** Bot is online but doesn't reply

**Solutions:**
- Verify bot token is correct
- Check firewall/network settings
- Restart the application

### Admin panel shows login error
**Problem:** Can't log in to admin panel

**Solutions:**
- Check `admin.username` and `admin.password` in config
- Clear browser cache
- Try incognito/private mode

### Database errors
**Problem:** JPA/Hibernate errors

**Solutions:**
- Delete `data/animesense.mv.db` and restart (resets database)
- Check database URL and credentials
- Verify database is running (for MySQL)

### API rate limit errors
**Problem:** "Too many requests" from Jikan API

**Solutions:**
- Increase `jikan.api.rate-limit-ms` (default 3000)
- Wait a few minutes before retrying
- Check Jikan API status

### Can't add anime
**Problem:** "Failed to fetch anime" error

**Solutions:**
- Verify MyAnimeList ID is correct
- Check internet connection
- Try again in a few seconds (rate limit)

---

## 📊 How It Works

### Recommendation Algorithm

**1. User Interaction**
```
User rates anime 8/10 with genres: Action, Adventure
↓
System updates taste profile:
  - Action weight: 1.0 → 1.1
  - Adventure weight: 1.0 → 1.1
```

**2. Getting Recommendations**
```
User requests "Smart" recommendations
↓
System:
  1. Gets user's top 3 genres
  2. Fetches anime for those genres
  3. Scores each anime:
     Score = (Rating × 10) + (Genre Matches × Weight × 20) × Boost
  4. Sorts by score
  5. Returns top 5
```

**3. Example Scoring**
```
Anime: Fullmetal Alchemist Brotherhood
  - MAL Rating: 9.1 → Base = 91
  - User's Action weight: 1.5 → +30
  - User's Adventure weight: 1.2 → +24
  - Admin boost: 1.0 → ×1.0
  - Featured: No → ×1.0
  
Final Score = (91 + 30 + 24) × 1.0 × 1.0 = 145
```

---

## 📈 Future Enhancements

- [ ] Collaborative filtering
- [ ] User-to-user recommendations
- [ ] Anime studio preferences
- [ ] Seasonal anime tracking
- [ ] Discord bot integration
- [ ] RESTful API for mobile apps
- [ ] Advanced analytics dashboard

---

## 📝 License

MIT License - feel free to use and modify!

---

## 🙏 Acknowledgments

- [Jikan API](https://jikan.moe/) - MyAnimeList unofficial API
- [TelegramBots](https://github.com/rubenlagus/TelegramBots) - Java Telegram Bot API
- [Spring Boot](https://spring.io/projects/spring-boot) - Application framework
- [MyAnimeList](https://myanimelist.net/) - Anime data source

---

## 📞 Support

Having issues? Check:
1. [Troubleshooting](#troubleshooting) section
2. Configuration in `application.properties`
3. Application logs

---

**Happy Watching! 🎬✨**
