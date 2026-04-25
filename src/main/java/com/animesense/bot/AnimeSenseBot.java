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

@Component
@Slf4j
public class AnimeSenseBot extends TelegramLongPollingBot {
    
    @Value("${telegram.bot.token}")
    private String botToken;
    
    @Value("${telegram.bot.username}")
    private String botUsername;
    
    private final UserService userService;
    private final AnimeService animeService;
    private final FavoriteService favoriteService;
    private final RecommendationService recommendationService;
    
    // User session storage (in production, use Redis)
    private final Map<Long, UserSession> userSessions = new HashMap<>();
    
    public AnimeSenseBot(UserService userService, AnimeService animeService, 
                        FavoriteService favoriteService, RecommendationService recommendationService) {
        this.userService = userService;
        this.animeService = animeService;
        this.favoriteService = favoriteService;
        this.recommendationService = recommendationService;
    }
    
    @Override
    public String getBotUsername() {
        return botUsername;
    }
    
    @Override
    public String getBotToken() {
        return botToken;
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
            "🎬 *Welcome to AnimeSense!*\n\n" +
            "Hey %s! I'm your personal anime recommendation bot. " +
            "I'll help you discover amazing anime based on your unique taste.\n\n" +
            "🧠 The more you use me, the smarter my recommendations become!\n\n" +
            "Use the menu below to get started:",
            firstName
        );
        
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText(message);
        sendMessage.setParseMode("Markdown");
        sendMessage.setReplyMarkup(getMainMenuKeyboard(userId));
        
        execute(sendMessage);
    }
    
    private void sendHelpMessage(Long chatId) throws TelegramApiException {
        String message = """
            ❓ *How to Use AnimeSense*
            
            *🎬 Get Recommendation*
            Get anime suggestions tailored to your taste
            
            *⭐ My Favorites*
            View and manage your anime collection
            
            *🔍 Search Anime*
            Find specific anime by name
            
            *📊 My Stats*
            See your watching statistics
            
            *💡 Tips:*
              • Rate anime to improve recommendations
              • Mark your watching status
              • The system learns from every interaction
            """;
        
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
        
        // Handle menu buttons
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
        message.setText("🎯 *Choose Recommendation Type*\n\nSelect how you'd like to discover anime:");
        message.setParseMode("Markdown");
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
        
        if (session.currentIndex >= session.recommendations.size()) {
            sendMessage(chatId, "✅ That's all for now! Get more recommendations from the menu.");
            return;
        }
        
        Anime anime = session.recommendations.get(session.currentIndex);
        boolean inFavorites = favoriteService.isInFavorites(userId, anime.getAnimeId());
        
        String message = formatAnimeInfo(anime, session.currentIndex + 1, session.recommendations.size());
        
        if (anime.getImageUrl() != null) {
            SendPhoto photo = new SendPhoto();
            photo.setChatId(chatId);
            photo.setPhoto(new InputFile(anime.getImageUrl()));
            photo.setCaption(message);
            photo.setParseMode("Markdown");
            photo.setReplyMarkup(getAnimeActionKeyboard(anime.getAnimeId(), inFavorites));
            execute(photo);
        } else {
            SendMessage msg = new SendMessage();
            msg.setChatId(chatId);
            msg.setText(message);
            msg.setParseMode("Markdown");
            msg.setReplyMarkup(getAnimeActionKeyboard(anime.getAnimeId(), inFavorites));
            execute(msg);
        }
    }
    
    private void showStatusSelection(Long chatId, Long animeId) throws TelegramApiException {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("➕ Select status:");
        message.setReplyMarkup(getStatusKeyboard(animeId));
        execute(message);
    }
    
    private void handleStatusSelection(Long chatId, Long userId, String data) throws TelegramApiException {
        String[] parts = data.replace("status_", "").split("_", 2);
        Long animeId = Long.parseLong(parts[0]);
        FavoriteStatus status = FavoriteStatus.valueOf(parts[1]);
        
        favoriteService.addToFavorites(userId, animeId, status);
        
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("✅ Added! Rate this anime (1-10):");
        message.setReplyMarkup(getRatingKeyboard(animeId));
        execute(message);
    }
    
    private void handleRating(Long chatId, Long userId, String data) throws TelegramApiException {
        String[] parts = data.replace("rate_", "").split("_");
        Long animeId = Long.parseLong(parts[0]);
        
        if (parts[1].equals("skip")) {
            showNextRecommendation(chatId, userId);
            return;
        }
        
        double rating = Double.parseDouble(parts[1]);
        favoriteService.updateRating(userId, animeId, rating);
        
        sendMessage(chatId, String.format("⭐ Rated %.0f/10!", rating));
        showNextRecommendation(chatId, userId);
    }
    
    private void showNextRecommendation(Long chatId, Long userId) throws TelegramApiException {
        UserSession session = getUserSession(userId);
        session.currentIndex++;
        showCurrentRecommendation(chatId, userId);
    }
    
    private void showFavorites(Long chatId, Long userId) throws TelegramApiException {
        List<Favorite> favorites = favoriteService.getUserFavorites(userId);
        
        if (favorites.isEmpty()) {
            sendMessage(chatId, "⭐ Your favorites list is empty!\n\nGet some recommendations and add anime you like.");
            return;
        }
        
        StringBuilder message = new StringBuilder("⭐ *Your Favorites*\n\n");
        
        Map<FavoriteStatus, List<Favorite>> grouped = new HashMap<>();
        for (Favorite fav : favorites) {
            grouped.computeIfAbsent(fav.getStatus(), k -> new ArrayList<>()).add(fav);
        }
        
        for (FavoriteStatus status : FavoriteStatus.values()) {
            List<Favorite> list = grouped.get(status);
            if (list != null && !list.isEmpty()) {
                message.append(String.format("*%s:*\n", status.getDisplayName()));
                for (Favorite fav : list.subList(0, Math.min(5, list.size()))) {
                    String rating = fav.getUserRating() != null ? String.format(" (%.0f/10)", fav.getUserRating()) : "";
                    message.append(String.format("  • %s%s\n", fav.getAnime().getTitle(), rating));
                }
                message.append("\n");
            }
        }
        
        message.append(String.format("_Total: %d anime_", favorites.size()));
        sendMessage(chatId, message.toString());
    }
    
    private void promptSearch(Long chatId, Long userId) throws TelegramApiException {
        UserSession session = getUserSession(userId);
        session.waitingFor = "search";
        sendMessage(chatId, "🔍 *Search Anime*\n\nSend me the name of the anime:");
    }
    
    private void handleSearchQuery(Long chatId, Long userId, String query) throws TelegramApiException {
        List<Anime> results = animeService.searchAnime(query);
        
        if (results.isEmpty()) {
            sendMessage(chatId, String.format("😕 No results found for '%s'", query));
            return;
        }
        
        Anime anime = results.get(0);
        boolean inFavorites = favoriteService.isInFavorites(userId, anime.getAnimeId());
        
        String message = String.format("🔍 *Search Results*\n\n%s", formatAnimeInfo(anime, 1, 1));
        
        SendMessage msg = new SendMessage();
        msg.setChatId(chatId);
        msg.setText(message);
        msg.setParseMode("Markdown");
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
            📊 *Your Stats*
            
            📚 Total: %d
            📺 Watching: %d
            ✅ Completed: %d
            📋 Plan to Watch: %d
            
            ⭐ Avg Rating: %.1f/10
            """, total, watching, completed, planToWatch, avgRating != null ? avgRating : 0.0);
        
        sendMessage(chatId, message);
    }
    
    private String formatAnimeInfo(Anime anime, int current, int total) {
        String title = anime.getTitle();
        if (anime.getTitleEnglish() != null && !anime.getTitleEnglish().equals(anime.getTitle())) {
            title += " (" + anime.getTitleEnglish() + ")";
        }
        
        String rating = anime.getRating() != null ? String.format("%.1f/10", anime.getRating()) : "Not rated";
        
        return String.format("""
            🎬 *Recommendation %d/%d*
            
            *%s*
            
            ⭐ Rating: %s
            🎭 Genres: %s
            📺 Episodes: %s
            
            %s
            """, current, total, title, rating, 
            anime.getGenres() != null ? anime.getGenres() : "Unknown",
            anime.getEpisodes() != null ? anime.getEpisodes() : "Unknown",
            anime.getSynopsis() != null ? anime.getSynopsis().substring(0, Math.min(300, anime.getSynopsis().length())) + "..." : "");
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
            
            if (i <= 5) {
                row1.add(button);
            } else {
                row2.add(button);
            }
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
        message.setText(text);
        message.setParseMode("Markdown");
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
