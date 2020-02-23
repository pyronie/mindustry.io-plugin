package mindustry.plugin;

import mindustry.Vars;
import mindustry.entities.type.Player;
import mindustry.gen.Call;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.permission.Role;
import org.json.JSONObject;

import mindustry.plugin.discordcommands.DiscordCommands;

import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;

import static mindustry.plugin.Utils.*;

public class BotThread extends Thread {
    public DiscordApi api;
    private Thread mt;
    private JSONObject data;
    public DiscordCommands commandHandler = new DiscordCommands();

    public BotThread(DiscordApi api, Thread mt, JSONObject data) {
        this.api = api; //new DiscordApiBuilder().setToken(data.get(0)).login().join();
        this.mt = mt;
        this.data = data;

        // register commands
        this.api.addMessageCreateListener(commandHandler);
        new ComCommands().registerCommands(commandHandler);
        new ServerCommands(data).registerCommands(commandHandler);
        //new MessageCreatedListeners(data).registerListeners(commandHandler);
    }

    public void run(){
        while (this.mt.isAlive()){
            try {
                Thread.sleep(60 * 1000);

                for (Player p : Vars.playerGroup.all()) {
                    // cooldowns
                    TempPlayerData tdata = (ioMain.playerDataGroup.getOrDefault(p.uuid, null));
                    if (tdata != null){
                        if (tdata.burstCD > 0){
                            tdata.burstCD--;
                        }
                    }
                    // increment playtime for users in-game
                    PlayerData pd = getData(p.uuid);
                    if (pd == null) return;

                    pd.playTime++;
                    if(pd.rank == 0 && pd.playTime >= activeRequirements.playtime && pd.buildingsBuilt >= activeRequirements.buildingsBuilt && pd.gamesPlayed >= activeRequirements.gamesPlayed){
                        Call.onInfoMessage(p.con, Utils.formatMessage(p, promotionMessage));
                        if (pd.rank < 1) pd.rank = 1;
                    }
                    setData(p.uuid, pd);
                }


            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        api.disconnect();
    }
}
