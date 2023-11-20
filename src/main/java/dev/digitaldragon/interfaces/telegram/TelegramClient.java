package dev.digitaldragon.interfaces.telegram;

import dev.digitaldragon.WikiBot;
import lombok.Getter;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

public class TelegramClient {
    @Getter
    private static boolean enabled = false;
    @Getter
    private static TelegramCommandsBot bot;

    public static void enable() {
        enabled = true;
        connect();
        WikiBot.getBus().register(new TelegramJobListener());
    }

    public static void connect() {
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            bot = new TelegramCommandsBot();
            botsApi.registerBot(bot);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
