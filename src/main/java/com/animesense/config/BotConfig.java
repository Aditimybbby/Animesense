package com.animesense.config;

import com.animesense.bot.AnimeSenseBot;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Configuration
@Slf4j
public class BotConfig {

    @Bean
    public TelegramBotsApi telegramBotsApi(AnimeSenseBot animeSenseBot) {
        try {
            TelegramBotsApi api = new TelegramBotsApi(DefaultBotSession.class);
            api.registerBot(animeSenseBot);
            log.info("✅ Telegram bot successfully registered and connected!");
            return api;
        } catch (TelegramApiException e) {
            log.error("❌ Failed to register Telegram bot", e);
            throw new RuntimeException("Failed to register bot", e);
        }
    }
}