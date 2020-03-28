package mindustry.plugin;

import arc.math.Mathf;
import arc.util.CommandHandler;
import mindustry.Vars;
import mindustry.entities.type.Player;
import mindustry.gen.Call;
import mindustry.plugin.commands.PublicCommands;
import mindustry.plugin.datas.PersistentPlayerData;
import mindustry.plugin.datas.PlayerData;
import mindustry.plugin.discord.ReactionAdd;
import mindustry.plugin.utils.Funcs;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Activity;
import org.json.JSONObject;

import mindustry.plugin.discord.DiscordCommands;

import static mindustry.Vars.netServer;
import static mindustry.Vars.playerGroup;
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
    public CommandHandler adminHandler = new CommandHandler(prefix);

    public BotThread(JDA api, Thread mt, JSONObject data) {
        this.api = api; //new DiscordApiBuilder().setToken(data.get(0)).login().join();
        this.mt = mt;
        this.data = data;

        // register commands
        api.addEventListener(commandHandler);
        api.addEventListener(reactionHandler);
        new PublicCommands().registerCommands(publicHandler);
    }

    public void run(){
        while (this.mt.isAlive()){
            try {
                Thread.sleep(60 * 1000);

                for (Player p : Vars.playerGroup.all()) {

                    PlayerData pd = getData(p.uuid);
                    if (pd == null) return;

                    // update buildings built
                    PersistentPlayerData tdata = (playerDataGroup.getOrDefault(p.uuid, null));
                    if (tdata != null){
                        if (tdata.bbIncrementor > 0){
                            pd.buildingsBuilt = pd.buildingsBuilt + tdata.bbIncrementor;
                            tdata.bbIncrementor = 0;
                        }
                    }


                    pd.playTime++;
                    if(pd.rank <= 0 && pd.playTime >= activeRequirements.playtime && pd.buildingsBuilt >= activeRequirements.buildingsBuilt && pd.gamesPlayed >= activeRequirements.gamesPlayed){
                        Call.onInfoMessage(p.con, Funcs.formatMessage(p, Funcs.promotionMessage));
                        if (pd.rank < 1) pd.rank = 1;
                    }
                    setData(p.uuid, pd);
                    playerDataGroup.put(p.uuid, tdata); // update tdata with the new stuff
                }
                if(Mathf.chance(0.01f)){
                    api.getPresence().setActivity(Activity.playing("( ͡° ͜ʖ ͡°)"));
                } else {
                    api.getPresence().setActivity(Activity.playing("with " + playerGroup.all().size + (netServer.admins.getPlayerLimit() == 0 ? "" : "/" + netServer.admins.getPlayerLimit()) + " players"));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        api.shutdown();
    }
}
