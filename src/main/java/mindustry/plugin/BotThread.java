package mindustry.plugin;

import arc.math.Mathf;
import arc.util.CommandHandler;
import mindustry.Vars;
import mindustry.entities.type.Player;
import mindustry.plugin.commands.ModeratorCommands;
import mindustry.plugin.commands.PublicCommands;
import mindustry.plugin.commands.ReviewerCommands;
import mindustry.plugin.datas.Achievements;
import mindustry.plugin.datas.PlayerData;
import mindustry.plugin.discord.ReactionAdd;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Activity;
import org.json.JSONObject;

import mindustry.plugin.discord.DiscordCommands;

import java.util.concurrent.CompletableFuture;

import static mindustry.Vars.netServer;
import static mindustry.Vars.playerGroup;
import static mindustry.plugin.ioMain.*;
import static mindustry.plugin.utils.Funcs.*;
import static mindustry.plugin.discord.Loader.*;

public class BotThread extends Thread {
    public JDA api;
    private Thread mt;
    private JSONObject data;
    public DiscordCommands commandHandler = new DiscordCommands();
    public ReactionAdd reactionHandler = new ReactionAdd();

    public CommandHandler publicHandler = new CommandHandler(prefix);
    public CommandHandler reviewerHandler = new CommandHandler(prefix);
    public CommandHandler moderatorHandler = new CommandHandler(prefix);

    public BotThread(JDA api, Thread mt, JSONObject data) {
        this.api = api; //new DiscordApiBuilder().setToken(data.get(0)).login().join();
        this.mt = mt;
        this.data = data;

        // register commands
        api.addEventListener(commandHandler);
        api.addEventListener(reactionHandler);

        new PublicCommands().registerCommands(publicHandler);
        new ReviewerCommands().registerCommands(reviewerHandler);
        new ModeratorCommands().registerCommands(moderatorHandler);
    }

    public void run(){
        while (this.mt.isAlive()){
            try {
                Thread.sleep(60 * 1000);

                for (Player p : Vars.playerGroup.all()) {
                    PlayerData pd = playerDataHashMap.get(p.uuid);
                    if (pd == null) return;
                    setJedisData(p.uuid, pd);
                }
                if(Mathf.chance(0.01f)){
                    api.getPresence().setActivity(Activity.playing("( ͡° ͜ʖ ͡°)"));
                } else {
                    api.getPresence().setActivity(Activity.playing("with " + playerGroup.all().size + (netServer.admins.getPlayerLimit() == 0 ? "" : "/" + netServer.admins.getPlayerLimit()) + " players"));
                }

                CompletableFuture.runAsync(() -> {
                    for(Achievements.Achievement achievement : achievementHandler.all){
                        achievement.onInterval();
                    }
                });

                minutesPassed++;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        api.shutdown();
    }
}
