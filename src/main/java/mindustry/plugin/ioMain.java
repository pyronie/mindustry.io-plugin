package mindustry.plugin;

import java.awt.*;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.IntStream;

import com.google.gson.Gson;
import arc.util.Timer;
import arc.util.Timer.Task;
import mindustry.content.*;
import mindustry.core.GameState;
import mindustry.entities.traits.Entity;
import mindustry.entities.type.BaseUnit;
import mindustry.graphics.Pal;
import mindustry.net.Administration;
import mindustry.world.Build;
import mindustry.world.Tile;
import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.channel.Channel;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.json.JSONObject;
import org.json.JSONTokener;

import arc.Core;
import arc.Events;
import arc.util.CommandHandler;
import arc.util.Log;
import mindustry.Vars;
import mindustry.entities.type.Player;
import mindustry.game.EventType;
import mindustry.gen.Call;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;

import static mindustry.Vars.*;
import static mindustry.plugin.Utils.*;

public class ioMain extends Plugin {
    public static JedisPool pool;
    static Gson gson = new Gson();

    public static DiscordApi api = null;
    public static String prefix = ".";
    public static String serverName = "<untitled>";
    
    //public static HashMap<String, PlayerData> database  = new HashMap<>(); // uuid, rank
    //public static HashMap<String, Boolean> verifiedIPs = new HashMap<>(); // uuid, verified?

    public static HashMap<String, TempPlayerData> playerDataGroup = new HashMap<>(); // uuid, data

    private final String fileNotFoundErrorMessage = "File not found: config\\mods\\settings.json";
    private JSONObject alldata;
    public static JSONObject data; //token, channel_id, role_id
    public static String apiKey = "";

    //register event handlers and create variables in the constructor
    public ioMain() {
        Utils.init();

        try {
            String pureJson = Core.settings.getDataDirectory().child("mods/settings.json").readString();
            data = alldata = new JSONObject(new JSONTokener(pureJson));
        } catch (Exception e) {
            Log.err("Couldn't read settings.json file.");
        }
        try {
            api = new DiscordApiBuilder().setToken(alldata.getString("token")).login().join();
        }catch (Exception e){
            Log.err("Couldn't log into discord.");
        }
        BotThread bt = new BotThread(api, Thread.currentThread(), alldata);
        bt.setDaemon(false);
        bt.start();



        // database
        try {
            pool = new JedisPool(new JedisPoolConfig(), "localhost");
            Log.info("jedis database loaded");
        } catch (Exception e){
            e.printStackTrace();
            Core.app.exit();
        }

        // setup prefix
        if (data.has("prefix")) {
            prefix = String.valueOf(data.getString("prefix").charAt(0));
        } else {
            Log.warn("Prefix not found, using default '.' prefix.");
        }
        api.updateActivity("prefix: " + prefix);

        // setup name
        if (data.has("server_name")) {
            serverName = String.valueOf(data.getString("server_name"));
        } else {
            Log.warn("No server name setting detected!");
        }

        if(data.has("api_key")){
            apiKey = data.getString("api_key");
            Log.info("apiKey set successfully");
        }

        // player joined
        Events.on(EventType.PlayerJoin.class, event -> {
            Player player = event.player;
            if(bannedNames.contains(player.name)) player.con.kick("[scarlet]Error #103, please join http://discord.mindustry.io and tell an admin.");
            PlayerData pd = getData(player.uuid);
            Thread verThread = new Thread(() -> {
                if(verification) {
                    if (pd != null && !pd.verified) {
                        Log.info("Unverified player joined: " + player.name);
                        Call.onInfoMessage(player.con, verificationMessage);
                    } else if (pd != null){ // if verified, but we wanna be extra cautious and check again :)
                        String url = "http://api.vpnblocker.net/v2/json/" + player.con.address + "/" + apiKey;
                        String pjson = ClientBuilder.newClient().target(url).request().accept(MediaType.APPLICATION_JSON).get(String.class);

                        JSONObject json = new JSONObject(new JSONTokener(pjson));
                        if (json.has("host-ip")) {
                            if (json.getBoolean("host-ip")) { // verification failed
                                Log.info("IP verification failed for: " + player.name);
                                pd.verified = false;
                                setData(player.uuid, pd);
                                Call.onInfoMessage(player.con, verificationMessage);
                                if (data.has("warnings_chat_channel_id")) {
                                    TextChannel tc = getTextChannel(data.getString("warnings_chat_channel_id"));
                                    if (tc != null) {
                                        EmbedBuilder eb = new EmbedBuilder().setTitle("IP verification failure for: " + escapeCharacters(player.name));
                                        eb.addField("IP", player.con.address);
                                        eb.addField("UUID", player.uuid);
                                        eb.setDescription("Verify this player by using the `" + prefix + "verify " + player.uuid + "` command.");
                                        eb.setColor(Pals.info);
                                        tc.sendMessage(eb);
                                    }
                                }
                            } else {
                                Log.info("IP verification success for: " + player.name);
                            }
                        }
                    }
                }
            });
            verThread.start();

            TempPlayerData tempData = playerDataGroup.get(player.uuid);
            if (tempData == null) {
                tempData = new TempPlayerData(player);
                playerDataGroup.put(player.uuid, tempData);
            } else {
                tempData.playerRef = new WeakReference<>(player);
                tempData.origName = player.name;
                tempData.doRainbow = false;
            }


            if(pd != null) {
                if(pd.banned) player.con.kick("uuid: " + player.uuid + " you are banned.");
                int rank = pd.rank;
                Administration.PlayerInfo info = netServer.admins.getInfoOptional(player.uuid);
                if(info == null) return;
                switch (rank) { // apply new tag
                    case 1:
                        Call.sendMessage("[sky]active player " + player.name + " joined the server!");
                        info.tag = rankNames.get(1) + " ";
                        break;
                    case 2:
                        Call.sendMessage("[#fcba03]regular player " + player.name + " joined the server!");
                        info.tag = rankNames.get(2) + " ";
                        break;
                    case 3:
                        Call.sendMessage("[scarlet]donator " + player.name + " joined the server!");
                        info.tag = rankNames.get(3) + " ";
                        break;
                    case 4:
                        Call.sendMessage("[orange]<[][white]io moderator[][orange]>[] " + player.name + " joined the server!");
                        info.tag = rankNames.get(4) + " ";
                        break;
                    case 5:
                        Call.sendMessage("[orange]<[][white]io admin[][orange]>[] " + player.name + " joined the server!");
                        info.tag = rankNames.get(5) + " ";
                        break;
                }
                tempData.origName = player.name;
            } else { // not in database
                setData(player.uuid, new PlayerData(0));
            }

            if(pd != null && !pd.ips.contains(player.con.address)) {
                pd.ips.add(player.con.address);
                setData(player.uuid, pd);
            }

            if (welcomeMessage.length() > 0) {
                Call.onInfoMessage(player.con, formatMessage(player, welcomeMessage));
            }
        });

        // player built building
        Events.on(EventType.BlockBuildEndEvent.class, event -> {
            if (event.player == null) return;
            if (event.breaking) return;
            PlayerData pd = getData(event.player.uuid);
            TempPlayerData td = (playerDataGroup.getOrDefault(event.player.uuid, null));
            if (pd == null || td == null) return;
            if (event.tile.entity != null) {
                if (!activeRequirements.bannedBlocks.contains(event.tile.block())) {
                    td.bbIncrementor++;
                }
            }
        });

        Events.on(EventType.GameOverEvent.class, event -> {
            for (Player p : playerGroup.all()) {
                PlayerData pd = getData(p.uuid);
                if (pd != null) {
                    pd.gamesPlayed++;
                    setData(p.uuid, pd);
                    Call.onInfoToast(p.con, "[accent]+1 games played", 10);
                }
            }
        });

        Events.on(EventType.WorldLoadEvent.class, event -> {
            Timer.schedule(MapRules::run, 5); // idk
        });

        Core.app.post(this::loop);
    }


    public void loop() {
        for (Entry<String, TempPlayerData> entry : playerDataGroup.entrySet()) {
            TempPlayerData tdata = entry.getValue();
            if (tdata == null) return;
            String uuid = entry.getKey();
            if (uuid == null) return;
            Player p = findPlayer(uuid);

            // update pets
            for (BaseUnit unit : tdata.draugPets) if (!unit.isAdded()) tdata.draugPets.remove(unit);

            if (p != null && tdata.doRainbow) {
                // update rainbows
                String playerNameUnmodified = tdata.origName;
                int hue = tdata.hue;
                if (hue < 360) {
                    hue = hue + 1;
                } else {
                    hue = 0;
                }

                String hex = "#" + Integer.toHexString(Color.getHSBColor(hue / 360f, 1f, 1f).getRGB()).substring(2);
                String[] c = playerNameUnmodified.split(" ", 2);
                if (c.length > 1) p.name = c[0] + " [" + hex + "]" + escapeColorCodes(c[1]);
                else p.name = "[" + hex + "]" + escapeColorCodes(c[0]);
                tdata.setHue(hue);
            }

            if (p != null && tdata.doTrail) {
                String hex = Integer.toHexString(Color.getHSBColor(tdata.hue / 360f, 1f, 1f).getRGB()).substring(2);

                arc.graphics.Color c = arc.graphics.Color.valueOf(hex);
                Call.onEffectReliable(Fx.shootLiquid, p.x, p.y, (180 + p.rotation)%360, c); // this inverse rotation thing gave me a headache
            }

            if(p != null && tdata.bt != null && p.isShooting()){
                Call.createBullet(tdata.bt, p.getTeam(), p.x, p.y, p.rotation, tdata.sclVelocity, tdata.sclLifetime);
            }
        }

        Core.app.post(this::loop);
    }

    //register commands that run on the server
    @Override
    public void registerServerCommands(CommandHandler handler){

    }

    //register commands that player can invoke in-game
    @Override
    public void registerClientCommands(CommandHandler handler){
        if (api != null) {
            handler.<Player>register("d", "<text...>", "Sends a message to moderators. Use when no moderators are online and there's a griefer.", (args, player) -> {

                if (!data.has("warnings_chat_channel_id")) {
                    player.sendMessage("[scarlet]This command is disabled.");
                } else {
                    TextChannel tc = getTextChannel(data.getString("warnings_chat_channel_id"));
                    if (tc == null) {
                        player.sendMessage("[scarlet]This command is disabled.");
                        return;
                    }
                    tc.sendMessage(escapeCharacters(player.name) + " *@mindustry* : `" + args[0] + "`");
                    player.sendMessage("[scarlet]Successfully sent message to moderators.");
                }

            });

            handler.<Player>register("players", "Display all players and their ids", (args, player) -> {
                StringBuilder builder = new StringBuilder();
                builder.append("[orange]List of players: \n");
                for (Player p : Vars.playerGroup.all()) {
                    if(p.isAdmin) {
                        builder.append("[accent]");
                    } else{
                        builder.append("[lightgray]");
                    }
                    builder.append(p.name).append("[accent] : ").append(p.id).append("\n");
                }
                player.sendMessage(builder.toString());
            });

            handler.<Player>register("rainbow", "[regular+]Give your username a rainbow animation", (args, player) -> {
                PlayerData pd = getData(player.uuid);
                if (pd != null && pd.rank > 2) {
                    TempPlayerData tdata = playerDataGroup.get(player.uuid);
                    if (tdata == null) return; // shouldn't happen, ever

                    player.sendMessage("[sky]Rainbow effect toggled.");
                    tdata.doRainbow = !tdata.doRainbow;
                } else {
                    player.sendMessage(noPermissionMessage);
                }
            });

            handler.<Player>register("trail", "[regular+]Give your username an in-world trail animation.", (args, player) -> {
                PlayerData pd = getData(player.uuid);
                if (pd != null && pd.rank > 2) {
                    TempPlayerData tdata = playerDataGroup.get(player.uuid);
                    if (tdata == null) return; // shouldn't happen, ever

                    player.sendMessage("[sky]Trail effect toggled.");
                    tdata.doTrail = !tdata.doTrail;
                } else {
                    player.sendMessage(noPermissionMessage);
                }
            });

            handler.<Player>register("draugpet", "[active+] Spawn a draug mining drone for your team (disabled on pvp)", (args, player) -> {
                if(!state.rules.pvp || player.isAdmin) {
                    PlayerData pd = getData(player.uuid);
                    if (pd != null && pd.rank >= 1) {
                        TempPlayerData tdata = playerDataGroup.get(player.uuid);
                        if (tdata == null) return;
                        if (tdata.draugPets.size < pd.rank || player.isAdmin) {
                            BaseUnit baseUnit = UnitTypes.draug.create(player.getTeam());
                            baseUnit.set(player.getX(), player.getY());
                            baseUnit.add();
                            tdata.draugPets.add(baseUnit);
                            Call.sendMessage(player.name + "[#b177fc] spawned in a draug pet! " + tdata.draugPets.size + "/" + pd.rank + " spawned.");
                        } else {
                            player.sendMessage("[#b177fc]You already have " + pd.rank + " draug pets active!");
                        }
                    } else {
                        player.sendMessage(noPermissionMessage);
                    }
                } else {
                    player.sendMessage("[scarlet]This command is disabled on pvp.");
                }
            });

            handler.<Player>register("lichpet", "[donator+] Spawn yourself a lich defense pet (max. 1 per game, lasts 2 minutes, disabled on pvp)", (args, player) -> {
                if(!state.rules.pvp || player.isAdmin) {
                    PlayerData pd = getData(player.uuid);
                    if (pd != null && pd.rank >= 3) {
                        TempPlayerData tdata = playerDataGroup.get(player.uuid);
                        if (tdata == null) return;
                        if (!tdata.spawnedLichPet || player.isAdmin) {
                            tdata.spawnedLichPet = true;
                            BaseUnit baseUnit = UnitTypes.lich.create(player.getTeam());
                            baseUnit.set(player.getClosestCore().x, player.getClosestCore().y);
                            baseUnit.health = 200f;
                            baseUnit.add();
                            Call.sendMessage(player.name + "[#ff0000] spawned in a lich defense pet! (lasts 2 minutes)");
                            Timer.schedule(baseUnit::kill, 120);
                        } else {
                            player.sendMessage("[#42a1f5]You already spawned a lich defense pet in this game!");
                        }
                    } else {
                        player.sendMessage(noPermissionMessage);
                    }
                } else {
                    player.sendMessage("[scarlet]This command is disabled on pvp.");
                }
            });

            handler.<Player>register("powergen", "[donator+] Spawn yourself a power generator.", (args, player) -> {
                if(!state.rules.pvp || player.isAdmin) {
                    PlayerData pd = getData(player.uuid);
                    if (pd != null && pd.rank >= 3) {
                        TempPlayerData tdata = playerDataGroup.get(player.uuid);
                        if (tdata == null) return;
                        if (!tdata.spawnedPowerGen || player.isAdmin) {
                            tdata.spawnedPowerGen = true;

                            float x = player.getX();
                            float y = player.getY();

                            Tile targetTile = world.tileWorld(x, y);

                            if (targetTile == null || !Build.validPlace(player.getTeam(), targetTile.x, targetTile.y, Blocks.rtgGenerator, 0)) {
                                Call.onInfoToast(player.con, "[scarlet]Cannot place a power generator here.",5f);
                                return;
                            }

                            targetTile.setNet(Blocks.rtgGenerator, player.getTeam(), 0);
                            Call.onLabel("[accent]" + escapeCharacters(escapeColorCodes(player.name)) + "'s[] generator", 60f, targetTile.worldx(), targetTile.worldy());
                            Call.onEffectReliable(Fx.explosion, targetTile.worldx(), targetTile.worldy(), 0, Pal.accent);
                            Call.onEffectReliable(Fx.placeBlock, targetTile.worldx(), targetTile.worldy(), 0, Pal.accent);
                            Call.sendMessage(player.name + "[#ff82d1] spawned in a power generator!");

                            // ok seriously why is this necessary
                            new Object() {
                                private Task task;
                                {
                                    task = Timer.schedule(() -> {
                                        if (targetTile.block() == Blocks.rtgGenerator) {
                                            Call.transferItemTo(Items.thorium, 1, targetTile.drawx(), targetTile.drawy(), targetTile);
                                        } else {
                                            player.sendMessage("[scarlet]Your power generator was destroyed!");
                                            task.cancel();
                                        }
                                    }, 0, 6);
                                }
                            };
                        } else {
                            player.sendMessage("[#ff82d1]You already spawned a power generator in this game!");
                        }
                    } else {
                        player.sendMessage(noPermissionMessage);
                    }
                } else {
                    player.sendMessage("[scarlet]This command is disabled on pvp.");
                }
            });

            handler.<Player>register("waterburst", "[donator+] Extinguish all ongoing fires (3 minute cooldown)", (args, player) -> {
                if(!state.rules.pvp || player.isAdmin) {
                    PlayerData pd = getData(player.uuid);
                    if (pd != null && pd.rank >= 3) {
                        TempPlayerData tdata = playerDataGroup.get(player.uuid);
                        if (tdata == null) return;
                        if (tdata.burstCD <= 0 || player.isAdmin) {
                            tdata.burstCD = 3;
                            float x = player.getX();
                            float y = player.getY();

                            Tile targetTile = world.tileWorld(x, y);
                            Call.onEffectReliable(Fx.healWave, targetTile.worldx(), targetTile.worldy(), 0, Pal.accent);

                            IntStream.range(0, 90).forEach(i -> {
                                Call.onEffectReliable(Fx.shootLiquid, targetTile.worldx(), targetTile.worldy(), i * 4, arc.graphics.Color.valueOf("65bdf7"));
                            });

                            for(Entity fire : fireGroup){
                                Call.onRemoveFire(fire.getID());
                            }
                            Call.sendMessage(player.name + "[#3279a8] extinguished all fires!");
                        } else {
                            player.sendMessage("[#3279a8]This command is on a cooldown. " + tdata.burstCD + "m remaining.");
                        }
                    } else {
                        player.sendMessage(noPermissionMessage);
                    }
                } else {
                    player.sendMessage("[scarlet]This command is disabled on pvp.");
                }
            });

            handler.<Player>register("spawn", "[active+]Skip the core spawning stage and spawn instantly.", (args, player) -> {
                if(!state.rules.pvp || player.isAdmin) {
                    PlayerData pd = getData(player.uuid);
                    if (pd != null && pd.rank >= 1) {
                        player.onRespawn(player.getClosestCore().tile);
                        player.sendMessage("[accent]Spawned!");
                    } else {
                        player.sendMessage(noPermissionMessage);
                    }
                } else {
                    player.sendMessage("[scarlet]This command is disabled on pvp.");
                }
            });

            handler.<Player>register("stats", "<player>", "Display stats of the specified player.", (args, player) -> {
                if(args[0].length() > 0) {
                    Player p = findPlayer(args[0]);
                    if(p != null){
                        PlayerData pd = getData(p.uuid);
                        if (pd != null) {
                            Call.onInfoMessage(player.con, formatMessage(p, statMessage));
                        }
                    } else {
                        player.sendMessage("[scarlet]Error: Player not found or offline");
                    }
                } else {
                    Call.onInfoMessage(player.con, formatMessage(player, statMessage));
                }
            });

            handler.<Player>register("info", "Display your stats.", (args, player) -> { // self info
                PlayerData pd = getData(player.uuid);
                if (pd != null) {
                    Call.onInfoMessage(player.con, formatMessage(player, statMessage));
                }
            });

            handler.<Player>register("label", "<duration> <text...>", "[admin only] Create an in-world label at the current position.", (args, player) -> {
                if(args[0].length() <= 0 || args[1].length() <= 0) player.sendMessage("[scarlet]Invalid arguments provided.");
                if (player.isAdmin) {
                    float x = player.getX();
                    float y = player.getY();

                    Tile targetTile = world.tileWorld(x, y);
                    Call.onLabel(args[1], Float.parseFloat(args[0]), targetTile.worldx(), targetTile.worldy());
                } else {
                    player.sendMessage(noPermissionMessage);
                }
            });

        }

    }

    public static TextChannel getTextChannel(String id){
        Optional<Channel> dc = api.getChannelById(id);
        if (!dc.isPresent()) {
            Log.err("[ERR!] discordplugin: channel not found! " + id);
            return null;
        }
        Optional<TextChannel> dtc = dc.get().asTextChannel();
        if (!dtc.isPresent()){
            Log.err("[ERR!] discordplugin: textchannel not found! " + id);
            return null;
        }
        return dtc.get();
    }

}