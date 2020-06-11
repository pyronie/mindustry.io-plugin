package mindustry.plugin;

import java.time.Instant;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import arc.math.Mathf;
import arc.math.geom.Geometry;
import arc.util.*;
import arc.util.Timer;
import mindustry.content.*;
import mindustry.game.Team;
import mindustry.graphics.Pal;
import mindustry.net.Administration;
import mindustry.plugin.datas.PlayerData;
import mindustry.plugin.discord.Loader;
import mindustry.plugin.utils.ContentHandler;
import mindustry.plugin.utils.MapRules;
import mindustry.plugin.utils.VoteSession;
import mindustry.world.Tile;

import arc.Events;
import mindustry.Vars;
import mindustry.entities.type.Player;
import mindustry.game.EventType;
import mindustry.gen.Call;
import mindustry.world.blocks.StaticWall;

import static mindustry.Vars.*;
import static mindustry.plugin.utils.Funcs.*;
import static mindustry.plugin.discord.Loader.*;

public class ioMain extends Plugin {
    public static HashMap<String, PlayerData> playerDataHashMap = new HashMap<>();
    public static int minutesPassed = 0;
    //register event handlers and create variables in the constructor
    public ioMain() {
        //we can load this before anything else, it doesnt matter
        Loader.load();
        content.load();

        // display on screen messages
        float duration = 10f;
        int start = 450;
        int increment = 30;

        Timer.schedule(() -> {
            int currentInc = 0;
            for(String msg : onScreenMessages){
                Call.onInfoPopup(msg, duration, 20, 50, 20, start + currentInc, 0);
                currentInc = currentInc + increment;
            }
        }, 0, 10);

        Events.on(EventType.ServerLoadEvent.class, event -> {
            contentHandler = new ContentHandler();
            Log.info("content loaded");
        });

        // update every tick
        Events.on(EventType.Trigger.update, () -> {

        });

        Events.on(EventType.TapEvent.class, tapEvent -> {
            if(tapEvent.tile != null) {
                Player player = tapEvent.player;
                Tile t = tapEvent.tile;
                player.tapTile = t;
                if (player.inspector) {
                    Call.onEffectReliable(player.con, Fx.placeBlock, t.worldx(), t.worldy(), 0.75f, Pal.accent);
                    player.sendMessage("[orange]--[] [accent]tile [](" + t.x + ", " + t.y + ")[accent] block:[] " + ((t.block() == null || t.block() == Blocks.air) ? "[#545454]none" : t.block().name) + " [orange]--[]");
                    Tile.tileInfo info = t.info;
                    if (info.placedBy != null) {
                        String pBy = (player.isAdmin ? info.placedByUUID : info.placedBy);
                        player.sendMessage("[accent]last placed by:[] " + escapeColorCodes(pBy));
                    }
                    if (info.destroyedBy != null) {
                        String dBy = (player.isAdmin ? info.destroyedByUUID : info.destroyedBy);
                        player.sendMessage("[accent]last [scarlet]deconstructed[] by:[] " + escapeColorCodes(dBy));
                    }
                    if (t.block() == Blocks.air && info.wasHere != null){
                        player.sendMessage("[accent]block that was here:[] " + info.wasHere);
                    }
                    if (info.configuredBy != null) {
                        String cBy = (player.isAdmin ? info.configuredByUUID : info.configuredBy);
                        player.sendMessage("[accent]last configured by:[] " + escapeColorCodes(cBy));
                    }
                }
            }
        });

        // player connected
        Events.on(EventType.PlayerConnect.class, event -> {
            Player player = event.player;
            PlayerData pd = playerDataHashMap.get(player.uuid);
            if (pd != null){
                if (pd.bannedUntil > Instant.now().getEpochSecond()){
                    player.con.kick("[scarlet]You are banned.[accent] Reason:\n" + pd.banReason, 0);
                }
            }
        });

        // player disconnected
        Events.on(EventType.PlayerLeave.class, event -> {
            String uuid = event.player.uuid;
            if(playerDataHashMap.containsKey(uuid))
                setJedisData(uuid, playerDataHashMap.get(uuid));

            //free ram
            playerDataHashMap.remove(uuid);
        });

        // player joined
        Events.on(EventType.PlayerJoin.class, event -> {
            CompletableFuture.runAsync(() -> {
                Player player = event.player;
                PlayerData pd = getJedisData(player.uuid);
                if(pd == null && playerDataHashMap.containsKey(player.uuid)){
                    pd = playerDataHashMap.get(player.uuid);
                }

                if (pd != null) {
                    if (pd.bannedUntil > Instant.now().getEpochSecond()) {
                        player.con.kick("[scarlet]You are banned.[accent] Reason:\n" + pd.banReason);
                        return;
                    }
                    if(pd.role > 0){
                        player.tag = rankNames.get(pd.role).tag;
                    }
                } else { // not in database
                    pd = new PlayerData();
                    setJedisData(player.uuid, new PlayerData());
                }
                playerDataHashMap.put(player.uuid, pd);

                if (welcomeMessage.length() > 0 && !welcomeMessage.equals("none")) {
                    Call.onInfoMessage(player.con, formatMessage(player, welcomeMessage));
                }

            });
        });


        Events.on(EventType.BuildSelectEvent.class, event -> {
            if(event.builder instanceof Player){
                if(event.tile != null){
                    Player player = (Player) event.builder;
                    Tile.tileInfo info = event.tile.info;
                    if(!event.breaking){
                        info.placedBy = player.name;
                        info.placedByUUID = player.uuid;
                        info.wasHere = (event.tile.block() != Blocks.air ? event.tile.block().name : "[#545454]none");
                    } else{
                        info.destroyedBy = player.name;
                        info.destroyedByUUID = player.uuid;
                    }
                }
            }
        });

        Events.on(EventType.TapConfigEvent.class, event -> {
            if(event.tile != null & event.player != null){
                Tile.tileInfo info = event.tile.info;
                Player player = event.player;
                info.configuredBy = player.name;
                info.configuredByUUID = player.uuid;
            }
        });

        Events.on(EventType.WorldLoadEvent.class, event -> {
            Timer.schedule(MapRules::run, 5); // idk
        });

        Events.on(EventType.ServerLoadEvent.class, event -> {
            // action filter
            Vars.netServer.admins.addActionFilter(action -> {
                Player player = action.player;
                if (player == null) return true;

                if (player.isAdmin) return true;
                if (!player.canInteract) return false;

                return action.type != Administration.ActionType.rotate;
            });
        });

        Events.on(EventType.Trigger.update, () -> {
            for(Player p : playerGroup.all()){
                if (p.bt != null && p.isShooting()) {
                    Call.createBullet(p.bt, p.getTeam(), p.getX(), p.getY(), p.rotation, p.sclVelocity, p.sclLifetime);
                }
            }
        });


        // thorium reactor terraforming
        Events.on(EventType.OverheatEvent.class, event -> {
            int radius = 15;
            AtomicBoolean got = new AtomicBoolean(false);
            Geometry.circle(event.tile.x, event.tile.y, radius, (cx, cy) -> {
                Tile t = world.tile(cx, cy);
                Timer.schedule(() -> {
                    if (t != null && t.block() instanceof StaticWall) {
                        got.set(true);
                        t.setNet(Blocks.air, Team.derelict, 0);
                        Call.onEffectReliable(Fx.explosion, t.worldx(), t.worldy(), 1, Pal.breakInvalid);
                    }
                }, (t != null ? t.dst(event.tile) / radius / 4 : 0));
            });
        });
    }

    //register commands that run on the server
    @Override
    public void registerServerCommands(CommandHandler handler){

    }

    //cooldown between map votes
    int voteCooldown = 120 * 1;

    //register commands that player can invoke in-game
    @Override
    public void registerClientCommands(CommandHandler handler){
        if (api != null) {

            handler.<Player>register("inspector", "Toggle on tile inspector. (Grief detection)", (args, player) -> {
                player.inspector = !player.inspector;
                player.sendMessage("[accent]Tile inspector toggled.");
            });

            handler.<Player>register("stats", "[player...]", "Display stats of the specified player (or yourself, if no player provided)", (args, player) -> {
                if(!statMessage.equals("none")) {
                    if (args.length <= 0) {
                        Call.onInfoMessage(player.con, formatMessage(player, statMessage));
                    } else {
                        Player p = findPlayer(args[0]);
                        if (p != null) {
                            Call.onInfoMessage(player.con, formatMessage(p, statMessage));
                        } else {
                            player.sendMessage("[lightgray]Can't find that player!");
                        }
                    }
                }
            });

            handler.<Player>register("event", "Join an ongoing event (if there is one)", (args, player) -> {
                if(eventIp.length() > 0){
                    Call.onConnect(player.con, eventIp, eventPort);
                } else{
                    player.sendMessage("[accent]There is no ongoing event at this time.");
                }
            });

            handler.<Player>register("maps","[page]", "Display all maps in the playlist.", (args, player) -> { // self info
                if(args.length > 0 && !Strings.canParseInt(args[0])){
                    player.sendMessage("[scarlet]'page' must be a number.");
                    return;
                }
                int perPage = 6;
                int page = args.length > 0 ? Strings.parseInt(args[0]) : 1;
                int pages = Mathf.ceil((float)Vars.maps.customMaps().size / perPage);

                page --;

                if(page >= pages || page < 0){
                    player.sendMessage("[scarlet]'page' must be a number between[orange] 1[] and[orange] " + pages + "[scarlet].");
                    return;
                }

                StringBuilder result = new StringBuilder();
                result.append(Strings.format("[orange]-- Maps Page[lightgray] {0}[gray]/[lightgray]{1}[orange] --\n\n", (page+1), pages));

                for(int i = perPage * page; i < Math.min(perPage * (page + 1), Vars.maps.customMaps().size); i++){
                    mindustry.maps.Map map = Vars.maps.customMaps().get(i);
                    result.append("[white] - [accent]").append(escapeColorCodes(map.name())).append("\n");
                }
                player.sendMessage(result.toString());
            });

            Timekeeper vtime = new Timekeeper(voteCooldown);
            VoteSession[] currentlyKicking = {null};

            handler.<Player>register("nominate","<map...>", "[regular+] Vote to change to a specific map.", (args, player) -> {
                if(!state.rules.pvp || player.isAdmin) {
                    mindustry.maps.Map found = getMapBySelector(args[0]);
                    if(found != null){
                        if(!vtime.get()){
                            player.sendMessage("[scarlet]You must wait " + voteCooldown/60 + " minutes between nominations.");
                            return;
                        }
                        VoteSession session = new VoteSession(currentlyKicking, found);

                        session.vote(player, 1);
                        vtime.reset();
                        currentlyKicking[0] = session;
                    }else{
                        player.sendMessage("[scarlet]No map[orange]'" + args[0] + "'[scarlet] found.");
                    }
                } else {
                    player.sendMessage("[scarlet]This command is disabled on pvp.");
                }
            });

            handler.<Player>register("rtv", "Vote to change the map.", (args, player) -> { // self info
                if(currentlyKicking[0] == null){
                    player.sendMessage("[scarlet]No map is being voted on.");
                }else{
                    //hosts can vote all they want
                    if(player.uuid != null && (currentlyKicking[0].voted.contains(player.uuid) || currentlyKicking[0].voted.contains(netServer.admins.getInfo(player.uuid).lastIP))){
                        player.sendMessage("[scarlet]You've already voted.");
                        return;
                    }

                    currentlyKicking[0].vote(player, 1);
                }
            });
        }

    }

}