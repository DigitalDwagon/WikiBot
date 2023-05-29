package dev.digitaldragon;

import lombok.Getter;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

import javax.security.auth.login.LoginException;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DokuWikiDumperBot {/*
    @Getter
    public static JDA instance;
    @Getter
    public static ExecutorService executorService = Executors.newFixedThreadPool(5);

    public static final GatewayIntent[] INTENTS = { GatewayIntent.DIRECT_MESSAGES,GatewayIntent.GUILD_MESSAGES, GatewayIntent.GUILD_MESSAGE_REACTIONS, GatewayIntent.GUILD_VOICE_STATES,GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_PRESENCES, GatewayIntent.GUILD_EMOJIS };


    public void main (String[] args) throws LoginException, InterruptedException {
        instance = JDABuilder.create("MTExMjI2MDMzMDI2MTg1MjIwMA.Gv5kdR.rxHTKmf_p5GSEZE3m5SEflg7Hyq2JwQFFG2DpY", Arrays.asList(INTENTS))
                .enableCache(CacheFlag.VOICE_STATE)
                .setStatus(OnlineStatus.DO_NOT_DISTURB)
                .addEventListeners()
                .build();


        instance.awaitReady();

        Guild testServer = instance.getGuildById("349920496550281226");
        if (testServer != null)
            testServer.upsertCommand("dokuwikiarchive", "Archive a DokuWiki using DokuWikiArchiver and upload to archive.org")
                    .addOption(OptionType.STRING, "url", "doku.php url for the wiki you want to archive", true)
                    .addOption(OptionType.STRING, "note", "Archiver's note. Displayed for your benefit", true)
                    .queue();

    }*/
}
