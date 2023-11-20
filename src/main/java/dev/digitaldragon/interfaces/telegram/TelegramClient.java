package dev.digitaldragon.interfaces.telegram;

import dev.digitaldragon.WikiBot;
import lombok.Getter;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

public class TelegramClient {
    @Getter
    public static boolean enabled = false;

    public static void enable() {
        enabled = true;
        connect();
        WikiBot.getBus().register(new TelegramCommandsBot());
    }

    public static void connect() {
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(new TelegramCommandsBot());
            //botsApi.registerBot(new TelegramNotifyBot());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
