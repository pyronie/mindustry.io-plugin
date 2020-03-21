import arc.Core;
import arc.Events;
import arc.util.*;
import mindustry.game.EventType;
import mindustry.plugin.Plugin;
import org.json.JSONObject;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;

public class ioMain extends Plugin {
    public boolean enabled = true;
    public String kickReason = "You are banned from the io server network.\nYou can't join.";
    
    //register event handlers and create variables in the constructor
    public ioMain() {
        enabled = Core.settings.getBool("io-global-bans-enabled", true);

        Events.on(EventType.PlayerConnect.class, event ->{
            if(!event.player.isAdmin) {
                String uuid = event.player.uuid;
                String url = "http://mindustry.io:8080/stats?uuid=" + uuid;
                try {
                    JSONObject json = new JSONObject(ClientBuilder.newClient().target(url).request().accept(MediaType.APPLICATION_JSON).get(String.class));
                    if(json.has("error") && !json.getBoolean("error")) {
                        JSONObject data = json.getJSONObject("data");
                        if(data.has("banned") && data.getBoolean("banned")){
                            event.player.con.kick(kickReason);
                            Log.info("<.io>: Kicking " + event.player.name + ".");
                        }
                    }
                } catch (Exception e) {
                    Log.info("<.io>: Connection with mindustry.io authentication server failed.");
                }
            }
        });
    }

    @Override
    public void registerServerCommands(CommandHandler handler){
        handler.register("iobans", "Toggle global io bans detection on and off.", (args, plr) -> {
            if(enabled){
                enabled = false;
                Core.settings.put("io-global-bans-enabled", false);
                Log.info("<.io>: io bans authentication disabled.");
            } else{
                enabled = true;
                Core.settings.put("io-global-bans-enabled", true);
                Log.info("<.io>: io bans authentication enabled.");
            }
        });
        handler.register("iobans-reason", "<text...>", "Change the kick reason a player gets if he's banned from the io servers.", (args, plr) -> {
            kickReason = args[0];
            Log.info("<.io>: kickReason changed to " + kickReason);
        });
    }

    @Override
    public void registerClientCommands(CommandHandler handler){

    }

}