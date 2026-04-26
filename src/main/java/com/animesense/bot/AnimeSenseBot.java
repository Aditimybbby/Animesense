package com.animesense.bot;

import com.animesense.model.Anime;
import com.animesense.model.Favorite;
import com.animesense.model.Favorite.FavoriteStatus;
import com.animesense.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class AnimeSenseBot extends TelegramLongPollingBot {

    private final UserService userService;
    private final AnimeService animeService;
    private final FavoriteService favoriteService;
    private final RecommendationService recommendationService;

    // User session storage (in production, use Redis)
    private final Map<Long, UserSession> userSessions = new ConcurrentHashMap<>();

    // FIXED: Pass token to super() constructor — without this the bot never connects to Telegram
    public AnimeSenseBot(@Value("${telegram.bot.token}") String botToken,
                         UserService userService, AnimeService animeService,
                         FavoriteService favoriteService, RecommendationService recommendationService) {
        super(botToken);
        this.userService = userService;
        this.animeService = animeService;
        this.favoriteService = favoriteService;
        this.recommendationService = recommendationService;
    }

    @Value("${telegram.bot.username}")
    private String botUsername;

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public void onUpdateReceived(Update update) {
        try {
            if (update.hasMessage() && update.getMessage().hasText()) {
                handleTextMessage(update);
            } else if (update.hasCallbackQuery()) {
                handleCallbackQuery(update);
            }
        } catch (Exception e) {
            log.error("Error processing update", e);
        }
    }

    private void handleTextMessage(Update update) throws TelegramApiException {
        Long userId = update.getMessage().getFrom().getId();
        String username = update.getMessage().getFrom().getUserName();
        String firstName = update.getMessage().getFrom().getFirstName();
        String text = update.getMessage().getText();

        userService.createOrUpdateUser(userId, username, firstName);

        if (text.startsWith("/")) {
            handleCommand(update, text);
        } else {
            handleUserInput(update, text);
        }
    }

    private void handleCommand(Update update, String command) throws TelegramApiException {
        Long chatId = update.getMessage().getChatId();
        Long userId = update.getMessage().getFrom().getId();

        switch (command.split(" ")[0]) {
            case "/start" -> sendWelcomeMessage(chatId, update.getMessage().getFrom().getFirstName(), userId);
            case "/help" -> sendHelpMessage(chatId);
            default -> sendMessage(chatId, "Unknown command. Use /help to see available commands.");
        }
    }

    private void sendWelcomeMessage(Long chatId, String firstName, Long userId) throws TelegramApiException {
        String message = String.format(
            "🎬 <b>Welcome to AnimeSense!</b>\n\n" +
            "Hey %s! I'm your personal anime recommendation bot. " +
            "I'll help you discover amazing anime based on your unique taste.\n\n" +
            "🧠 The more you use me, the smarter my recommendations become!\n\n" +
            "Use the menu below to get started:",
            firstName
        );

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText(message);
        sendMessage.setParseMode("HTML"); // FIXED
        sendMessage.setReplyMarkup(getMainMenuKeyboard(userId));

        execute(sendMessage);
    }

    private void sendHelpMessage(Long chatId) throws TelegramApiException {
        String message = """
            ❓ <b>How to Use AnimeSense</b>
            
            <b>🎬 Get Recommendation</b>
            Get anime suggestions tailored to your taste
            
            <b>⭐ My Favorites</b>
            View and manage your anime collection
            
            <b>🔍 Search Anime</b>
            Find specific anime by name
            
            <b>📊 My Stats</b>
            See your watching statistics
            
            <b>💡 Tips:</b>
              • Rate anime to improve recommendations
              • Mark your watching status
              • The system learns from every interaction
            """; // FIXED

        sendMessage(chatId, message);
    }

    private void handleUserInput(Update update, String text) throws TelegramApiException {
        Long chatId = update.getMessage().getChatId();
        Long userId = update.getMessage().getFrom().getId();

        UserSession session = userSessions.get(userId);

        if (session != null && session.waitingFor != null) {
            switch (session.waitingFor) {
                case "search" -> handleSearchQuery(chatId, userId, text);
            }
            session.waitingFor = null;
            return;
        }

        switch (text) {
            case "🎬 Get Recommendation" -> showRecommendationType(chatId);
            case "⭐ My Favorites" -> showFavorites(chatId, userId);
            case "🔍 Search Anime" -> promptSearch(chatId, userId);
            case "📊 My Stats" -> showStats(chatId, userId);
            case "❓ Help" -> sendHelpMessage(chatId);
            default -> handleSearchQuery(chatId, userId, text);
        }
    }

    private void handleCallbackQuery(Update update) throws TelegramApiException {
        String data = update.getCallbackQuery().getData();
        Long chatId = update.getCallbackQuery().getMessage().getChatId();
        Long userId = update.getCallbackQuery().getFrom().getId();

        if (data.startsWith("rec_")) {
            handleRecommendationType(chatId, userId, data);
        } else if (data.startsWith("add_")) {
            Long animeId = Long.parseLong(data.substring(4));
            showStatusSelection(chatId, animeId);
        } else if (data.startsWith("status_")) {
            handleStatusSelection(chatId, userId, data);
        } else if (data.startsWith("rate_")) {
            handleRating(chatId, userId, data);
        } else if (data.equals("next_rec")) {
            showNextRecommendation(chatId, userId);
        }
    }

    private void showRecommendationType(Long chatId) throws TelegramApiException {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("🎯 <b>Choose Recommendation Type</b>\n\nSelect how you'd like to discover anime:"); // FIXED
        message.setParseMode("HTML"); // FIXED
        message.setReplyMarkup(getRecommendationTypeKeyboard());
        execute(message);
    }

    private void handleRecommendationType(Long chatId, Long userId, String data) throws TelegramApiException {
        String mode = data.replace("rec_", "");
        List<Anime> recommendations = recommendationService.getRecommendations(userId, mode, 5);

        if (recommendations.isEmpty()) {
            sendMessage(chatId, "😕 No recommendations found. Try a different mode!");
            return;
        }

        UserSession session = getUserSession(userId);
        session.recommendations = recommendations;
        session.currentIndex = 0;

        showCurrentRecommendation(chatId, userId);
    }

    private void showCurrentRecommendation(Long chatId, Long userId) throws TelegramApiException {
        UserSession session = getUserSession(userId);

        if (session.recommendations.isEmpty() || session.currentIndex >= session.recommendations.size()) {
            sendMessage(chatId, "😕 No more recommendations!");
            return;
        }

        Anime anime = session.recommendations.get(session.currentIndex);
        boolean inFavorites = favoriteService.isInFavorites(userId, anime.getAnimeId());

        String message = formatAnimeInfo(anime, session.currentIndex + 1, session.recommendations.size());

        if (anime.getImageUrl() != null && !anime.getImageUrl().isEmpty()) {
            SendPhoto photo = new SendPhoto();
            photo.setChatId(chatId);
            photo.setPhoto(new InputFile(anime.getImageUrl()));
            photo.setCaption(message);
            photo.setParseMode("HTML"); // FIXED
            photo.setReplyMarkup(getAnimeActionKeyboard(anime.getAnimeId(), inFavorites));
            execute(photo);
        } else {
            SendMessage msg = new SendMessage();
            msg.setChatId(chatId);
            msg.setText(message);
            msg.setParseMode("HTML"); // FIXED
            msg.setReplyMarkup(getAnimeActionKeyboard(anime.getAnimeId(), inFavorites));
            execute(msg);
        }
    }

    private void showNextRecommendation(Long chatId, Long userId) throws TelegramApiException {
        UserSession session = getUserSession(userId);
        session.currentIndex++;
        showCurrentRecommendation(chatId, userId);
    }

    private void showStatusSelection(Long chatId, Long animeId) throws TelegramApiException {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("📋 <b>Select Status</b>\n\nHow would you like to track this anime?"); // FIXED
        message.setParseMode("HTML"); // FIXED
        message.setReplyMarkup(getStatusKeyboard(animeId));
        execute(message);
    }

    private void handleStatusSelection(Long chatId, Long userId, String data) throws TelegramApiException {
        String[] parts = data.split("_", 3);
        if (parts.length < 3) return;

        Long animeId = Long.parseLong(parts[1]);
        FavoriteStatus status = FavoriteStatus.valueOf(parts[2]);

        favoriteService.addToFavorites(userId, animeId, status);
        sendMessage(chatId, "✅ Added to your favorites as <b>" + status.getDisplayName() + "</b>!\n\nRate this anime (1-10)?"); // FIXED

        SendMessage ratingMsg = new SendMessage();
        ratingMsg.setChatId(chatId);
        ratingMsg.setText("⭐ Rate this anime:");
        ratingMsg.setReplyMarkup(getRatingKeyboard(animeId));
        execute(ratingMsg);
    }

    private void handleRating(Long chatId, Long userId, String data) throws TelegramApiException {
        String[] parts = data.split("_", 3);
        if (parts.length < 3) return;

        Long animeId = Long.parseLong(parts[1]);
        String ratingStr = parts[2];

        if (!ratingStr.equals("skip")) {
            double rating = Double.parseDouble(ratingStr);
            favoriteService.updateRating(userId, animeId, rating);
            sendMessage(chatId, "⭐ Rating saved! Your taste profile has been updated.");
        } else {
            sendMessage(chatId, "Skipped rating. You can rate it later from your favorites.");
        }
    }

    private void showFavorites(Long chatId, Long userId) throws TelegramApiException {
        var favorites = favoriteService.getUserFavorites(userId);

        if (favorites.isEmpty()) {
            sendMessage(chatId, "⭐ <b>My Favorites</b>\n\nYou haven't added any anime yet!\n\nUse 🎬 Get Recommendation to discover anime.");
            return;
        }

        StringBuilder sb = new StringBuilder("⭐ <b>My Favorites</b> (" + favorites.size() + " anime)\n\n");

        favorites.stream().limit(10).forEach(fav -> {
            sb.append("• <b>").append(fav.getAnime().getTitle()).append("</b>\n");
            sb.append("  Status: ").append(fav.getStatus().getDisplayName());
            if (fav.getUserRating() != null) {
                sb.append(" | Rating: ").append(fav.getUserRating()).append("/10");
            }
            sb.append("\n\n");
        });

        if (favorites.size() > 10) {
            sb.append("<i>...and ").append(favorites.size() - 10).append(" more</i>");
        }

        sendMessage(chatId, sb.toString());
    }

    private void promptSearch(Long chatId, Long userId) throws TelegramApiException {
        UserSession session = getUserSession(userId);
        session.waitingFor = "search";
        sendMessage(chatId, "🔍 <b>Search Anime</b>\n\nEnter the anime name you're looking for:"); // FIXED
    }

    private void handleSearchQuery(Long chatId, Long userId, String query) throws TelegramApiException {
        List<Anime> results = animeService.searchAnime(query);

        if (results.isEmpty()) {
            sendMessage(chatId, String.format("😕 No results found for '%s'", query));
            return;
        }

        Anime anime = results.get(0);
        boolean inFavorites = favoriteService.isInFavorites(userId, anime.getAnimeId());

        String message = String.format("🔍 <b>Search Results</b>\n\n%s", formatAnimeInfo(anime, 1, 1)); // FIXED

        SendMessage msg = new SendMessage();
        msg.setChatId(chatId);
        msg.setText(message);
        msg.setParseMode("HTML"); // FIXED
        msg.setReplyMarkup(getAnimeActionKeyboard(anime.getAnimeId(), inFavorites));
        execute(msg);
    }

    private void showStats(Long chatId, Long userId) throws TelegramApiException {
        long total = favoriteService.getUserFavoriteCount(userId);

        if (total == 0) {
            sendMessage(chatId, "📊 You haven't added any anime yet!");
            return;
        }

        long watching = favoriteService.getUserFavoriteCountByStatus(userId, FavoriteStatus.WATCHING);
        long completed = favoriteService.getUserFavoriteCountByStatus(userId, FavoriteStatus.COMPLETED);
        long planToWatch = favoriteService.getUserFavoriteCountByStatus(userId, FavoriteStatus.PLAN_TO_WATCH);
        Double avgRating = favoriteService.getUserAverageRating(userId);

        String message = String.format("""
            📊 <b>Your Stats</b>
            
            📚 Total: %d
            📺 Watching: %d
            ✅ Completed: %d
            📋 Plan to Watch: %d
            
            ⭐ Avg Rating: %.1f/10
            """, total, watching, completed, planToWatch, avgRating != null ? avgRating : 0.0); // FIXED

        sendMessage(chatId, message);
    }

    private String formatAnimeInfo(Anime anime, int current, int total) {
        String title = anime.getTitle();
        if (anime.getTitleEnglish() != null && !anime.getTitleEnglish().equals(anime.getTitle())) {
            title += " (" + anime.getTitleEnglish() + ")";
        }

        String rating = anime.getRating() != null ? String.format("%.1f/10", anime.getRating()) : "Not rated";

        // FIXED: Escape HTML tags to prevent crashes
        String synopsis = anime.getSynopsis() != null ? 
            anime.getSynopsis().substring(0, Math.min(300, anime.getSynopsis().length())).replace("<", "&lt;").replace(">", "&gt;") + "..." : "";

        return String.format("""
            🎬 <b>Recommendation %d/%d</b>
            
            <b>%s</b>
            
            ⭐ Rating: %s
            🎭 Genres: %s
            📺 Episodes: %s
            
            %s
            """, current, total, title, rating,
            anime.getGenres() != null ? anime.getGenres() : "Unknown",
            anime.getEpisodes() != null ? anime.getEpisodes() : "Unknown",
            synopsis); // FIXED
    }

    private ReplyKeyboardMarkup getMainMenuKeyboard(Long userId) {
        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        List<KeyboardRow> rows = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        row1.add(new KeyboardButton("🎬 Get Recommendation"));
        rows.add(row1);

        KeyboardRow row2 = new KeyboardRow();
        row2.add(new KeyboardButton("⭐ My Favorites"));
        row2.add(new KeyboardButton("🔍 Search Anime"));
        rows.add(row2);

        KeyboardRow row3 = new KeyboardRow();
        row3.add(new KeyboardButton("📊 My Stats"));
        row3.add(new KeyboardButton("❓ Help"));
        rows.add(row3);

        keyboard.setKeyboard(rows);
        keyboard.setResizeKeyboard(true);
        return keyboard;
    }

    private InlineKeyboardMarkup getRecommendationTypeKeyboard() {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        rows.add(List.of(InlineKeyboardButton.builder()
            .text("🧠 Smart (Based on my taste)")
            .callbackData("rec_smart")
            .build()));

        rows.add(List.of(InlineKeyboardButton.builder()
            .text("🔥 Trending (Popular now)")
            .callbackData("rec_trending")
            .build()));

        rows.add(List.of(InlineKeyboardButton.builder()
            .text("🎲 Random")
            .callbackData("rec_basic")
            .build()));

        markup.setKeyboard(rows);
        return markup;
    }

    private InlineKeyboardMarkup getAnimeActionKeyboard(Long animeId, boolean inFavorites) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        if (inFavorites) {
            rows.add(List.of(InlineKeyboardButton.builder()
                .text("✅ Already in Favorites")
                .callbackData("noop")
                .build()));
        } else {
            rows.add(List.of(InlineKeyboardButton.builder()
                .text("➕ Add to Favorites")
                .callbackData("add_" + animeId)
                .build()));
        }

        rows.add(List.of(InlineKeyboardButton.builder()
            .text("➡️ Next")
            .callbackData("next_rec")
            .build()));

        markup.setKeyboard(rows);
        return markup;
    }

    private InlineKeyboardMarkup getStatusKeyboard(Long animeId) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        for (FavoriteStatus status : FavoriteStatus.values()) {
            rows.add(List.of(InlineKeyboardButton.builder()
                .text(status.getDisplayName())
                .callbackData("status_" + animeId + "_" + status.name())
                .build()));
        }

        markup.setKeyboard(rows);
        return markup;
    }

    private InlineKeyboardMarkup getRatingKeyboard(Long animeId) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        List<InlineKeyboardButton> row1 = new ArrayList<>();
        List<InlineKeyboardButton> row2 = new ArrayList<>();

        for (int i = 1; i <= 10; i++) {
            InlineKeyboardButton button = InlineKeyboardButton.builder()
                .text(String.valueOf(i))
                .callbackData("rate_" + animeId + "_" + i)
                .build();

            if (i <= 5) row1.add(button);
            else row2.add(button);
        }

        rows.add(row1);
        rows.add(row2);
        rows.add(List.of(InlineKeyboardButton.builder()
            .text("⏭️ Skip Rating")
            .callbackData("rate_" + animeId + "_skip")
            .build()));

        markup.setKeyboard(rows);
        return markup;
    }

    private void sendMessage(Long chatId, String text) throws TelegramApiException {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        
        // FIXED: Safely convert any accidental leftover Markdown asterisks to HTML bold tags
        String safeText = text.replaceAll("\\*(.*?)\\*", "<b>$1</b>");
        // Convert any leftover Markdown underscores to HTML italic tags
        safeText = safeText.replaceAll("_(.*?)_", "<i>$1</i>");
        
        message.setText(safeText);
        message.setParseMode("HTML"); // FIXED
        execute(message);
    }

    private UserSession getUserSession(Long userId) {
        return userSessions.computeIfAbsent(userId, k -> new UserSession());
    }

    private static class UserSession {
        List<Anime> recommendations = new ArrayList<>();
        int currentIndex = 0;
        String waitingFor;
    }
}