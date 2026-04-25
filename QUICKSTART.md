# 🚀 AnimeSense - Quick Start Guide

## Step 1: Get Bot Token from Telegram

1. Open Telegram
2. Search for `@BotFather`
3. Send `/newbot`
4. Follow the prompts:
   - Name: `AnimeSense Bot` (or any name you like)
   - Username: `myanimesense_bot` (must end with 'bot')
5. **Copy the bot token** (looks like: `123456:ABC-DEF1234ghIkl-zyx57W2v1u123ew11`)

## Step 2: Get Your Telegram User ID

1. Search for `@userinfobot` on Telegram
2. Send `/start`
3. **Copy your ID** (e.g., `123456789`)

## Step 3: Configure the Application

Open `src/main/resources/application.properties` and update:

```properties
# Replace YOUR_BOT_TOKEN_HERE with your actual token
telegram.bot.token=YOUR_BOT_TOKEN_HERE

# Replace with your bot's username
telegram.bot.username=myanimesense_bot

# Replace YOUR_TELEGRAM_USER_ID with your ID from step 2
admin.telegram.ids=YOUR_TELEGRAM_USER_ID

# Change default admin password (IMPORTANT!)
admin.password=changeme123
```

## Step 4: Build and Run

### Option A: Using Maven (Recommended)

```bash
# Make sure you're in the project directory
cd animesense-java

# Build the project
mvn clean package

# Run the application
java -jar target/animesense-bot-1.0.0.jar
```

### Option B: Run with Maven directly

```bash
mvn spring-boot:run
```

## Step 5: Test the Bot

1. Open Telegram
2. Search for your bot username (e.g., `@myanimesense_bot`)
3. Click "Start" or send `/start`
4. You should see a welcome message!

## Step 6: Access Admin Panel

1. Open your browser
2. Go to: `http://localhost:8080/admin/login`
3. Login with:
   - Username: `admin`
   - Password: `changeme123` (or what you set in step 3)

## Step 7: Add Your First Anime

1. In the admin panel, click "Anime" in the sidebar
2. Click "➕ Add Anime"
3. Enter a MyAnimeList ID (e.g., `52991` for Sousou no Frieren)
4. Click "Add Anime"

**Where to find anime IDs:**
- Go to https://myanimelist.net
- Search for any anime
- Look at the URL: `https://myanimelist.net/anime/52991/...`
- The number `52991` is the ID!

## Step 8: Get Recommendations

1. Go back to Telegram
2. Click "🎬 Get Recommendation"
3. Choose "🔥 Trending"
4. Start discovering anime!

---

## ✅ You're Done!

The bot is now running and ready to recommend anime!

### What's Next?

- **Rate anime** to build your taste profile
- **Try Smart recommendations** after adding 5+ favorites
- **Set featured anime** in admin panel to boost them
- **Send broadcasts** to announce new anime

---

## 🐛 Troubleshooting

### Bot doesn't start?
- Check your bot token is correct
- Make sure Java 17+ is installed: `java -version`

### Bot doesn't respond?
- Verify the token in `application.properties`
- Check the application is running (look for logs in terminal)

### Can't login to admin panel?
- Check username/password in `application.properties`
- Try: `admin` / `changeme123`

### Build fails?
- Make sure Maven is installed: `mvn -version`
- Try: `mvn clean install -U`

---

## 📖 Need More Help?

Check the full README.md for:
- Detailed configuration options
- Production deployment guides
- Advanced features
- Database setup

---

**Happy Watching! 🎬**
