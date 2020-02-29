package mindustry.plugin;

import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.content.Bullets;
import mindustry.content.Mechs;
import mindustry.content.UnitTypes;
import mindustry.entities.bullet.BulletType;
import mindustry.entities.type.BaseUnit;
import mindustry.entities.Damage;
import mindustry.game.Teams;
import mindustry.graphics.Pal;
import mindustry.net.Administration;
import mindustry.plugin.discordcommands.Command;
import mindustry.plugin.discordcommands.Context;
import mindustry.plugin.discordcommands.DiscordCommands;
import mindustry.plugin.discordcommands.RoleRestrictedCommand;
import arc.Core;
import arc.Events;
import arc.files.Fi;
import arc.struct.Array;

import mindustry.core.GameState;
import mindustry.entities.type.Player;
import mindustry.game.EventType.GameOverEvent;
import mindustry.game.Team;
import mindustry.gen.Call;
import mindustry.maps.Map;
import mindustry.maps.Maps;
import mindustry.io.SaveIO;

import mindustry.server.ServerControl;
import mindustry.type.Mech;
import mindustry.type.UnitType;
import mindustry.world.Block;
import mindustry.world.Tile;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.MessageAttachment;

import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.json.JSONObject;

import java.io.*;
import java.lang.reflect.Field;
import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;
import java.util.zip.InflaterInputStream;

import static mindustry.Vars.*;
import static mindustry.plugin.Utils.*;

public class ServerCommands {

    private JSONObject data;

    public ServerCommands(JSONObject data){
        this.data = data;
    }

    public void registerCommands(DiscordCommands handler) {

        handler.registerCommand(new Command("maps") {
            {
                help = "Check a list of available maps and their ids.";
            }
            public void run(Context ctx) {
                StringBuilder msg = new StringBuilder().append("**All available maps in the playlist:**\n```");
                Array<Map> mapList = maps.customMaps();
                for (int i = 0; i < mapList.size; i++) {
                    Map m = mapList.get(i);
                    msg.append(i).append(" : ").append(m.name()).append(" : ").append(m.width).append(" x ").append(m.height).append("\n");
                }
                msg.append("```");
                ctx.channel.sendMessage(msg.toString());
            }
        });
        if (data.has("administrator_roleid")) {
            String adminRole = data.getString("administrator_roleid");
            handler.registerCommand(new RoleRestrictedCommand("changemap"){
                {
                    help = "<mapname/mapid> Change the current map to the one provided.";
                    role = adminRole;
                }

                public void run(Context ctx) {
                    EmbedBuilder eb = new EmbedBuilder();
                    if (ctx.args.length < 2) {
                        eb.setTitle("Command terminated.");
                        eb.setColor(Pals.error);
                        eb.setDescription("Not enough arguments, use `%changemap <mapname|mapid>`".replace("%", ioMain.prefix));
                        ctx.channel.sendMessage(eb);
                        return;
                    }
                    Map found = getMapBySelector(ctx.message.trim());
                    if (found == null) {
                        eb.setTitle("Command terminated.");
                        eb.setColor(Pals.error);
                        eb.setDescription("Map \"" + escapeCharacters(ctx.message.trim()) + "\" not found!");
                        ctx.channel.sendMessage(eb);
                        return;
                    }

                    changeMap(found);

                    eb.setTitle("Command executed.");
                    eb.setDescription("Changed map to " + found.name());
                    ctx.channel.sendMessage(eb);

                    maps.reload();
                }
            });

            handler.registerCommand(new RoleRestrictedCommand("setrank"){
                {
                    help = "<playerid|ip|name> <rank> Change the player's rank to the provided one.";
                    role = adminRole;
                }

                public void run(Context ctx) {
                    EmbedBuilder eb = new EmbedBuilder();
                    String target = ctx.args[1];
                    int targetRank = Integer.parseInt(ctx.args[2]);
                    if(target.length() > 0 && targetRank > -1 && targetRank < 6) {
                        Player player = findPlayer(target);
                        if(player == null){
                            eb.setTitle("Command terminated");
                            eb.setDescription("Player not found.");
                            eb.setColor(Pals.error);
                            ctx.channel.sendMessage(eb);
                            return;
                        }

                        PlayerData pd = getData(player.uuid);
                        if(pd != null) {
                            pd.rank = targetRank;
                            setData(player.uuid, pd);
                            eb.setTitle("Command executed successfully");
                            eb.setDescription("Promoted " + escapeCharacters(player.name) + " to " + targetRank);
                            ctx.channel.sendMessage(eb);
                            player.con.kick("Your rank was modified, please rejoin.", 0);
                        }

                        if(targetRank==5) netServer.admins.adminPlayer(player.uuid, player.usid);
                    }
                }

            });
        }

        if (data.has("exit_roleid")) {
            handler.registerCommand(new RoleRestrictedCommand("exit") {
                {
                    help = "Close the server.";
                    role = data.getString("exit_roleid");
                }
                public void run(Context ctx) {
                    net.dispose();
                    Core.app.exit();
                }
            });
        }
        if (data.has("moderator_roleid")) {
            String banRole = data.getString("moderator_roleid");

            handler.registerCommand(new RoleRestrictedCommand("announce") {
                {
                    help = "<message> Announces a message to in-game chat.";
                    role = banRole;
                }
                public void run(Context ctx) {
                    EmbedBuilder eb = new EmbedBuilder();

                    if (ctx.message.length() <= 0) {
                        eb.setTitle("Command terminated");
                        eb.setColor(Pals.error);
                        eb.setDescription("No message given");
                        ctx.channel.sendMessage(eb);
                        return;
                    }

                    for (Player p : playerGroup.all()) {
                        Call.onInfoToast(p.con, ctx.message, 10);
                    }

                    eb.setTitle("Command executed");
                    eb.setDescription("Your message was announced.");
                    ctx.channel.sendMessage(eb);

                }
            });

            handler.registerCommand(new RoleRestrictedCommand("event") {
                {
                    help = "<ip/none> Changes the event command ip.";
                    role = banRole;
                }
                public void run(Context ctx) {
                    EmbedBuilder eb = new EmbedBuilder();

                    if (ctx.message.length() <= 0) {
                        eb.setTitle("Command terminated");
                        eb.setColor(Pals.error);
                        eb.setDescription("No message given");
                        ctx.channel.sendMessage(eb);
                        return;
                    }

                    if(ctx.message.toLowerCase().contains("none")){
                        eventIp = "";
                        eb.setTitle("Command executed");
                        eb.setDescription("Event command is now disabled.");
                        ctx.channel.sendMessage(eb);
                        return;
                    }

                    String[] m = ctx.message.split(":");
                    eventIp = m[0];
                    eventPort = Integer.parseInt(m[1]);

                    eb.setTitle("Command executed");
                    eb.setDescription("Event ip was changed to " + ctx.message);
                    ctx.channel.sendMessage(eb);
                }
            });

            handler.registerCommand(new RoleRestrictedCommand("alert") {
                {
                    help = "<playerid|ip|name|teamid> <message> Alerts a player(s) using on-screen messages.";
                    role = banRole;
                }
                public void run(Context ctx) {
                    EmbedBuilder eb = new EmbedBuilder();
                    String target = ctx.args[1].toLowerCase();

                    if(target.equals("all")) {
                        for (Player p : playerGroup.all()) {
                            Call.onInfoMessage(p.con, ctx.message.split(" ", 2)[1]);
                        }
                        eb.setTitle("Command executed");
                        eb.setDescription("Alert was sent to all players.");
                        ctx.channel.sendMessage(eb);
                    } else if(target.matches("[0-9]+") && target.length()==1){
                        for(Player p : playerGroup.all()){
                            if(p.getTeam().id== Byte.parseByte(target)){
                                Call.onInfoMessage(p.con, ctx.message.split(" ", 2)[1]);
                            }
                        }
                        eb.setTitle("Command executed");
                        eb.setDescription("Alert was sent to all players.");
                        ctx.channel.sendMessage(eb);
                    } else{
                        Player p = findPlayer(target);
                        if (p != null) {
                            Call.onInfoMessage(p.con, ctx.message.split(" ", 2)[1]);
                            eb.setTitle("Command executed");
                            eb.setDescription("Alert was sent to " + escapeCharacters(p.name));
                            ctx.channel.sendMessage(eb);
                        } else {
                            eb.setTitle("Command terminated");
                            eb.setColor(Pals.error);
                            eb.setDescription("Player could not be found or is offline.");
                            ctx.channel.sendMessage(eb);

                        }
                    }
                }
            });

            /*handler.registerCommand(new RoleRestrictedCommand("sethudtext") {
                {
                    help = "<playerid|ip|name> <message> Sets the HUD text";
                    role = banRole;
                }
                public void run(Context ctx) {
                    EmbedBuilder eb = new EmbedBuilder();
                    String target = ctx.args[1].toLowerCase();

                    if(target.equals("all")) {
                        for (Player p : playerGroup.all()) {
                            Call.setHudText(p.con, ctx.message.split(" ", 2)[1]);
                        }
                        eb.setTitle("Command executed");
                        eb.setDescription("Alert was sent to all players.");
                        ctx.channel.sendMessage(eb);
                    } else{
                        Player p = findPlayer(target);
                        if (p != null) {
                            Call.setHudText(p.con, ctx.message.split(" ", 2)[1]);
                            eb.setTitle("Command executed");
                            eb.setDescription("Alert was sent to " + escapeCharacters(p.name));
                            ctx.channel.sendMessage(eb);
                        } else {
                            eb.setTitle("Command terminated");
                            eb.setColor(Pals.error);
                            eb.setDescription("Player could not be found or is offline.");
                            ctx.channel.sendMessage(eb);
                        }
                    }
                }
            });*/

            handler.registerCommand(new RoleRestrictedCommand("gameover") {
                {
                    help = "Force a game over.";
                    role = banRole;
                }
                public void run(Context ctx) {
                    if (state.is(GameState.State.menu)) {
                        ctx.reply("Invalid state");
                        return;
                    }
                    Events.fire(new GameOverEvent(Team.crux));
                    EmbedBuilder eb = new EmbedBuilder()
                            .setTitle("Command executed.")
                            .setDescription("Done. New game starting in 10 seconds.");
                    ctx.channel.sendMessage(eb);
                }
            });

            handler.registerCommand(new RoleRestrictedCommand("ban") {
                {
                    help = "<ip/id> Ban a player by the provided ip or id.";
                    role = banRole;
                }
 
                public void run(Context ctx) {
                    EmbedBuilder eb = new EmbedBuilder()
                            .setTimestampToNow();
                    String target = ctx.args[1];
                    if (target.length() > 0) {
                        Player p = findPlayer(target);
                        if (p != null) {
                            PlayerData pd = getData(p.uuid);
                            if (pd != null){
                                pd.banned = true;
                                setData(p.uuid, pd);
                            }
                            netServer.admins.banPlayer(p.uuid);
                            eb.setTitle("Command executed.");
                            eb.setDescription("Banned " + p.name + "(#" + p.id + ") `" + p.con.address + "` successfully!");
                            ctx.channel.sendMessage(eb);
                            p.con.kick("You've been banned by: " + ctx.author.getName() + ". Appeal at http://discord.mindustry.io");
                            Call.sendMessage("[scarlet]" + escapeCharacters(p.name) + " has been banned permanently.");
                        } else {
                            eb.setTitle("Command terminated");
                            eb.setColor(Pals.error);
                            eb.setDescription("Player not online. Use %blacklist <ip> to ban an offline player.".replace("%", ioMain.prefix));
                            ctx.channel.sendMessage(eb);
                        }
                    } else {
                        eb.setTitle("Command terminated");
                        eb.setColor(Pals.error);
                        eb.setDescription("Not enough arguments / usage: `%ban <id/ip>`".replace("%", ioMain.prefix));
                        ctx.channel.sendMessage(eb);
                    }
                }
            });

            handler.registerCommand(new RoleRestrictedCommand("blacklist") {
                {
                    help = "<ip> Ban a player by the provided ip.";
                    role = banRole;
                }

                public void run(Context ctx) {
                    EmbedBuilder eb = new EmbedBuilder()
                            .setTimestampToNow();
                    String target = ctx.args[1];
                    if (target.length() > 0) {
                        netServer.admins.banPlayerIP(target);
                        eb.setTitle("Blacklisted successfully.");
                        eb.setDescription("`" + target + "` was banned.");
                        ctx.channel.sendMessage(eb);
                    } else {
                        eb.setTitle("Command terminated");
                        eb.setColor(Pals.error);
                        eb.setDescription("Not enough arguments / usage: `%blacklist <ip>`".replace("%", ioMain.prefix));
                        ctx.channel.sendMessage(eb);
                    }
                }
            });

            handler.registerCommand(new RoleRestrictedCommand("unban") {
                EmbedBuilder eb = new EmbedBuilder();
                {
                    help = "<ip/uuid> Unban a player by the provided ip or uuid.";
                    role = banRole;
                }
                public void run(Context ctx) {
                    String target;
                    if(ctx.args.length==2){ target = ctx.args[1]; } else {ctx.reply("Invalid arguments provided, use the following format: %unban <ip>".replace("%", ioMain.prefix)); return;}

                    if (netServer.admins.unbanPlayerIP(target)) {
                        eb.setTitle("Command executed");
                        eb.setDescription("Unbanned `" + target + "` successfully");
                    } else {
                        PlayerData pd = getData(target);
                        if (pd != null){
                            pd.banned = false;
                            setData(target, pd);
                            eb.setTitle("Command executed");
                            eb.setDescription(target + " was unbanned successfully.");
                        } else {
                            eb.setTitle("Command terminated");
                            eb.setColor(Pals.error);
                            eb.setDescription("No such ban exists.");
                        }
                    }
                    ctx.channel.sendMessage(eb);
                }
            });

            handler.registerCommand(new RoleRestrictedCommand("unvotekick") {
                EmbedBuilder eb = new EmbedBuilder();
                {
                    help = "<id> Unvotekickban the specified player";
                    role = banRole;
                }
                public void run(Context ctx) {
                    String target = ctx.args[1];
                    Administration.PlayerInfo info = netServer.admins.getInfo(target);

                    if (info != null) {
                        info.lastKicked = 0;
                        eb.setTitle("Command executed.");
                        eb.setDescription("Unvotekickbanned `" + target + "` succeessfully.");
                        ctx.channel.sendMessage(eb);
                    } else {
                        eb.setTitle("Command terminated.");
                        eb.setColor(Pals.error);
                        eb.setDescription("That ID isn't votekickbanned!");
                        ctx.channel.sendMessage(eb);
                    }
                }
            });

            handler.registerCommand(new RoleRestrictedCommand("kick") {
                EmbedBuilder eb = new EmbedBuilder()
                        .setTimestampToNow();
                {
                    help = "<ip/id> Kick a player by the provided ip or id.";
                    role = banRole;
                }
                public void run(Context ctx) {
                    String target = ctx.args[1];

                    Player p = findPlayer(target);
                    if (p != null) {
                        eb.setTitle("Command executed.");
                        eb.setDescription("Kicked " + p.name + "(#" + p.id + ") `" + p.con.address + "` successfully.");
                        Call.sendMessage("[scarlet]" + escapeCharacters(p.name) + " has been kicked.");
                        p.con.kick("You've been kicked by: " + ctx.author.getName());
                    } else {
                        eb.setTitle("Command terminated.");
                        eb.setColor(Pals.error);
                        eb.setDescription("Player not online / not found.");
                    }
                    ctx.channel.sendMessage(eb);
                }
            });

            handler.registerCommand(new RoleRestrictedCommand("playersinfo") {
                {
                    help = "Check the information about all players on the server.";
                    role = banRole;
                }
                public void run(Context ctx) {
                    StringBuilder msg = new StringBuilder("**Players online: " + playerGroup.size() + "**\n```\n");
                    for (Player player : playerGroup.all()) {
                        msg.append("· ").append(escapeCharacters(player.name));
                        if(!player.isAdmin) {
                            msg.append(" : ").append(player.con.address).append(" : ").append(player.uuid).append("\n");
                        } else {
                            msg.append("\n");
                        }
                    }
                    msg.append("```");

                    ctx.channel.sendMessage(msg.toString());
                }
            });

            handler.registerCommand(new RoleRestrictedCommand("uploadmap") {
                {
                    help = "<.msav attachment> Upload a new map (Include a .msav file with command message)";
                    role = banRole;
                }
                public void run(Context ctx) {
                    EmbedBuilder eb = new EmbedBuilder();
                    Array<MessageAttachment> ml = new Array<>();
                    for (MessageAttachment ma : ctx.event.getMessageAttachments()) {
                        if (ma.getFileName().split("\\.", 2)[1].trim().equals("msav")) {
                            ml.add(ma);
                        }
                    }
                    if (ml.size != 1) {
                        eb.setTitle("Map upload terminated.");
                        eb.setColor(Pals.error);
                        eb.setDescription("You need to add one valid .msav file!");
                        ctx.channel.sendMessage(eb);
                        return;
                    } else if (Core.settings.getDataDirectory().child("maps").child(ml.get(0).getFileName()).exists()) {
                        eb.setTitle("Map upload terminated.");
                        eb.setColor(Pals.error);
                        eb.setDescription("There is already a map with this name on the server!");
                        ctx.channel.sendMessage(eb);
                        return;
                    }
                    // more custom filename checks possible

                    CompletableFuture<byte[]> cf = ml.get(0).downloadAsByteArray();
                    Fi fh = Core.settings.getDataDirectory().child("maps").child(ml.get(0).getFileName());

                    try {
                        byte[] data = cf.get();
                        if (!SaveIO.isSaveValid(new DataInputStream(new InflaterInputStream(new ByteArrayInputStream(data))))) {
                            eb.setTitle("Map upload terminated.");
                            eb.setColor(Pals.error);
                            eb.setDescription("Map file corrupted or invalid.");
                            ctx.channel.sendMessage(eb);
                            return;
                        }
                        fh.writeBytes(cf.get(), false);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    maps.reload();
                    eb.setTitle("Map upload completed.");
                    eb.setDescription(ml.get(0).getFileName() + " was added succesfully into the playlist!");
                    ctx.channel.sendMessage(eb);
                    //Utils.LogAction("uploadmap", "Uploaded a new map", ctx.author, null);
                }
            });
            handler.registerCommand(new RoleRestrictedCommand("removemap") {
                {
                    help = "<mapname/mapid> Remove a map from the playlist (use mapname/mapid retrieved from the %maps command)".replace("%", ioMain.prefix);
                    role = banRole;
                }
                @Override
                public void run(Context ctx) {
                    EmbedBuilder eb = new EmbedBuilder();
                    if (ctx.args.length < 2) {
                        eb.setTitle("Command terminated.");
                        eb.setColor(Pals.error);
                        eb.setDescription("Not enough arguments, use `%removemap <mapname/mapid>`".replace("%", ioMain.prefix));
                        ctx.channel.sendMessage(eb);
                        return;
                    }
                    Map found = getMapBySelector(ctx.message.trim());
                    if (found == null) {
                        eb.setTitle("Command terminated.");
                        eb.setColor(Pals.error);
                        eb.setDescription("Map not found");
                        ctx.channel.sendMessage(eb);
                        return;
                    }

                    maps.removeMap(found);
                    maps.reload();

                    eb.setTitle("Command executed.");
                    eb.setDescription(found.name() + " was successfully removed from the playlist.");
                    ctx.channel.sendMessage(eb);
                }
            });
            handler.registerCommand(new RoleRestrictedCommand("syncserver") {
                {
                    help = "Tell everyone to resync.";
                    role = banRole;
                }
                public void run(Context ctx) {
                    for(Player p : playerGroup.all()) {
                        Call.onWorldDataBegin(p.con);
                        netServer.sendWorldData(p);
                    }
                    EmbedBuilder eb = new EmbedBuilder()
                            .setTitle("Command executed.")
                            .setDescription("Synchronized every player's client with the server.");
                    ctx.channel.sendMessage(eb);
                }
            });

            handler.registerCommand(new RoleRestrictedCommand("mech") {
                {
                    help = "<mechname> <playerid|ip|all|name|teamid> Change the provided player into a specific mech.";
                    role = banRole;
                }
                public void run(Context ctx) {
                    String target = ctx.args[1];
                    String targetMech = ctx.args[2];
                    Mech desiredMech = Mechs.alpha;
                    if(target.length() > 0 && targetMech.length() > 0) {
                        try {
                            Field field = Mechs.class.getDeclaredField(targetMech);
                            desiredMech = (Mech)field.get(null);
                        } catch (NoSuchFieldException | IllegalAccessException ignored) {}

                        EmbedBuilder eb = new EmbedBuilder();

                        if(target.equals("all")) {
                            for (Player p : playerGroup.all()) {
                                p.mech = desiredMech;
                            }
                            eb.setTitle("Command executed successfully.");
                            eb.setDescription("Changed everyone's mech into " + desiredMech.name);
                            ctx.channel.sendMessage(eb);
                            return;
                        }
                        else if(target.matches("[0-9]+") && target.length()==1){
                        for(Player p : playerGroup.all()){
                            if(p.getTeam().id== Byte.parseByte(target)){
                                p.mech = desiredMech;
                            }
                        }
                            eb.setTitle("Command executed successfully.");
                            eb.setDescription("Changed everyone's mech into " + desiredMech.name);
                            ctx.channel.sendMessage(eb);
                        return;
                    }
                        Player player = findPlayer(target);
                        if(player!=null){
                            player.mech = desiredMech;
                            eb.setTitle("Command executed successfully.");
                            eb.setDescription("Changed " + escapeCharacters(player.name) + "s mech into " + desiredMech.name);
                            ctx.channel.sendMessage(eb);
                        }
                    }
                }
            });

            handler.registerCommand(new RoleRestrictedCommand("changeteam") {
                {
                    help = "<playerid|ip|all|name|teamid> <team> Change the provided player's team into the provided one.";
                    role = banRole;
                }
                public void run(Context ctx) {
                    String target = ctx.args[1];
                    String targetTeam = ctx.args[2];
                    Team desiredTeam = Team.crux;


                    if(target.length() > 0 && targetTeam.length() > 0) {
                        try {
                            Field field = Team.class.getDeclaredField(targetTeam);
                            desiredTeam = (Team)field.get(null);
                        } catch (NoSuchFieldException | IllegalAccessException ignored) {}

                        EmbedBuilder eb = new EmbedBuilder();
                        if(target.equals("all")) {
                            for (Player p : playerGroup.all()) {
                                p.setTeam(desiredTeam);
                            }
                            eb.setTitle("Command executed successfully.");
                            eb.setDescription("Changed everyone's team to " + desiredTeam.name);
                            ctx.channel.sendMessage(eb);
                            return;
                        }
                        else if(target.matches("[0-9]+") && target.length()==1){
                        for(Player p : playerGroup.all()){
                            if(p.getTeam().id== Byte.parseByte(target)){
                                p.setTeam(desiredTeam);
                            }
                        }
                        eb.setTitle("Command executed successfully.");
                            eb.setDescription("Changed everyone's team to " + desiredTeam.name);
                            ctx.channel.sendMessage(eb);
                            return;
                    }
                        Player player = findPlayer(target);
                        if(player!=null){
                            player.setTeam(desiredTeam);
                            eb.setTitle("Command executed successfully.");
                            eb.setDescription("Changed " + escapeCharacters(player.name) + "s team to " + desiredTeam.name);
                            ctx.channel.sendMessage(eb);
                        }
                    }
                }
            });

            handler.registerCommand(new RoleRestrictedCommand("changeteamid") {
                {
                    help = "<playerid|ip|all|name> <team> Change the provided player's team into a generated int.";
                    role = banRole;
                }
                public void run(Context ctx) {
                    String target = ctx.args[1];
                    int targetTeam = Integer.parseInt(ctx.args[2]);
                    if(target.length() > 0 && targetTeam > 0) {
                        EmbedBuilder eb = new EmbedBuilder();

                        if(target.equals("all")) {
                            for (Player p : playerGroup.all()) {
                                p.setTeam(Team.get(targetTeam));
                            }
                            eb.setTitle("Command executed successfully.");
                            eb.setDescription("Changed everyone's team to " + targetTeam);
                            ctx.channel.sendMessage(eb);
                            return;
                        }
                        else if(target.matches("[0-9]+") && target.length()==1){
                        for(Player p : playerGroup.all()){
                            if(p.getTeam().id == Byte.parseByte(target)){
                                p.setTeam(Team.get(targetTeam));
                            }
                        }
                        eb.setTitle("Command executed successfully.");
                            eb.setDescription("Changed everyone's team to " + targetTeam);
                            ctx.channel.sendMessage(eb);
                            return;
                    }
                        Player player = findPlayer(target);
                        if(player!=null){
                            player.setTeam(Team.get(targetTeam));
                            eb.setTitle("Command executed successfully.");
                            eb.setDescription("Changed " + escapeCharacters(player.name) + "s team to " + targetTeam);
                            ctx.channel.sendMessage(eb);
                        }
                    }
                }
            });

            handler.registerCommand(new RoleRestrictedCommand("rename"){
                {
                    help = "<playerid|ip|name> <name> Rename the provided player";
                    role = banRole;
                }

                public void run(Context ctx) {
                    EmbedBuilder eb = new EmbedBuilder();
                    String target = ctx.args[1];
                    String name = ctx.message.substring(target.length() + 1);
                    if(target.length() > 0 && name.length() > 0) {
                        Player player = findPlayer(target);
                        if (player != null) {
                            player.name = name;
                            TempPlayerData tdata = ioMain.playerDataGroup.get(player.uuid);
                            if (tdata != null) tdata.origName = name;
                            eb.setTitle("Command executed successfully");
                            eb.setDescription("Changed name to " + escapeCharacters(player.name));
                            ctx.channel.sendMessage(eb);
                            Call.onInfoToast(player.con, "[scarlet]Your name was changed by a moderator.", 10);
                        }
                    }
                }

            });

            handler.registerCommand(new RoleRestrictedCommand("verify"){
                {
                    help = "<uuid> Verify the provided UUID and allow them to join the server.";
                    role = banRole;
                }

                public void run(Context ctx) {
                    EmbedBuilder eb = new EmbedBuilder();
                    String target = ctx.args[1];
                    PlayerData pd = getData(target);
                    if(pd != null) {
                        pd.verified = true;
                        setData(target, pd);
                        eb.setTitle("Command executed successfully");
                        eb.setDescription("Verified " + escapeCharacters(target) + ".");
                    } else {
                        eb.setTitle("Command terminated");
                        eb.setDescription("Couldn't find " + escapeCharacters(target) + " in the database.");
                        eb.setColor(Pals.error);
                    } ctx.channel.sendMessage(eb);
                }

            });

            handler.registerCommand(new RoleRestrictedCommand("motd"){
                {
                    help = "<newmessage> Change / set a welcome message";
                    role = banRole;
                }

                public void run(Context ctx) {
                    EmbedBuilder eb = new EmbedBuilder();
                    eb.setTitle("Command executed successfully");
                    String message = ctx.message;
                    if(message.length() > 0 && !message.equals("disable")) {
                        welcomeMessage = message;
                        Core.settings.put("welcomeMessage", message);
                        eb.setDescription("Changed welcome message.");
                        ctx.channel.sendMessage(eb);
                    } else {
                        eb.setDescription("Disabled welcome message.");
                        ctx.channel.sendMessage(eb);
                    }
                }

            });

            handler.registerCommand(new RoleRestrictedCommand("screenmessage") {
                {
                    help = "<list/remove/add> <message> List, remove or add on-screen messages.";
                    role = banRole;
                }
                public void run(Context ctx) {
                    EmbedBuilder eb = new EmbedBuilder();
                    String target = ctx.args[1].toLowerCase();
                    String message = "";
                    if(!target.equals("list")) {
                        message = ctx.message.split(" ", 2)[1];
                    }

                    switch (target) {
                        case "list":
                            eb.setTitle("All on-screen messages:");
                            for (String msg : onScreenMessages) {
                                eb.addField(String.valueOf(onScreenMessages.indexOf(msg)), msg);
                            }
                            ctx.channel.sendMessage(eb);
                            break;

                        case "remove":
                            if (onScreenMessages.get(Integer.parseInt(message.trim())) != null) {
                                onScreenMessages.remove(Integer.parseInt(message.trim()));
                                eb.setTitle("Command executed");
                                eb.setDescription("Removed provided on-screen message.");
                            } else {
                                eb.setTitle("Command terminated");
                                eb.setDescription("That on-screen message does not exist.");
                                eb.setColor(Pals.error);
                            }
                            ctx.channel.sendMessage(eb);
                            break;

                        case "add":
                            if (message.length() > 0) {
                                onScreenMessages.add(message);
                                eb.setTitle("Command executed");
                                eb.setDescription("Added on-screen message `" + message + "`.");
                            } else {
                                eb.setTitle("Command terminated");
                                eb.setDescription("On-screen messages must be longer than 0 characters.");
                                eb.setColor(Pals.error);
                            }
                            ctx.channel.sendMessage(eb);
                            break;

                        default:
                            eb.setTitle("Command terminated");
                            eb.setDescription("Invalid arguments provided.");
                            eb.setColor(Pals.error);
                            ctx.channel.sendMessage(eb);
                            break;
                    }
                }
            });

            handler.registerCommand(new RoleRestrictedCommand("statmessage"){
                {
                    help = "<newmessage> Change / set a stat message";
                    role = banRole;
                }

                public void run(Context ctx) {
                    EmbedBuilder eb = new EmbedBuilder();
                    eb.setTitle("Command executed successfully");
                    String message = ctx.message;
                    if(message.length() > 0) {
                        statMessage = message;
                        Core.settings.put("statMessage", message);
                        eb.setDescription("Changed stat message.");
                        ctx.channel.sendMessage(eb);
                    } else {
                        eb.setTitle("Command terminated");
                        eb.setDescription("No message provided.");
                        ctx.channel.sendMessage(eb);
                    }
                }

            });


            handler.registerCommand(new RoleRestrictedCommand("spawn") {
                {
                    help = "<playerid|ip|name> <unit> <amount> Spawn x units at the location of the specified player";
                    role = banRole;
                }
                public void run(Context ctx) {
                    EmbedBuilder eb = new EmbedBuilder();
                    String target = ctx.args[1];
                    String targetUnit = ctx.args[2];
                    int amount = Integer.parseInt(ctx.args[3]);
                    UnitType desiredUnit = UnitTypes.dagger;
                    if(target.length() > 0 && targetUnit.length() > 0 && amount > 0 && amount < 1000) {
                        try {
                            Field field = UnitTypes.class.getDeclaredField(targetUnit);
                            desiredUnit = (UnitType)field.get(null);
                        } catch (NoSuchFieldException | IllegalAccessException ignored) {}

                        Player player = findPlayer(target);
                        if(player!=null){
                            UnitType finalDesiredUnit = desiredUnit;
                            IntStream.range(0, amount).forEach(i -> {
                                BaseUnit unit = finalDesiredUnit.create(player.getTeam());
                                unit.set(player.getX(), player.getY());
                                unit.add();
                            });
                            eb.setTitle("Command executed successfully.");
                            eb.setDescription("Spawned " + amount + " " + targetUnit + " near " + Utils.escapeCharacters(player.name) + ".");
                            ctx.channel.sendMessage(eb);
                        }
                    } else{
                        eb.setTitle("Command terminated");
                        eb.setDescription("Invalid arguments provided.");
                        eb.setColor(Pals.error);
                        ctx.channel.sendMessage(eb);
                    }
                }
            });
            handler.registerCommand(new RoleRestrictedCommand("boom") {
                {
                    help = "<playerid|ip|name> <flammability> <explosiveness> <power> <radius> Explodes with given specifications at given players position";
                    role = banRole;
                } // Pal.darkFlame
                public void run(Context ctx) {
                    
                    String target = ctx.args[1];
                    int flammability = Integer.parseInt(ctx.args[2]);
                    int explosiveness = Integer.parseInt(ctx.args[3]);
                    int power = Integer.parseInt(ctx.args[4]);
                    int radius = Integer.parseInt(ctx.args[5]);
                    if(target.length() > 0  && flammability > 0 && flammability< 1000 && explosiveness > 0 && explosiveness< 1000 && power > 0 && power< 1000 && radius > 0 && radius< 1000) {
                        

                        EmbedBuilder eb = new EmbedBuilder();
                        Player player = findPlayer(target);
                        if(player!=null){
                            Damage.dynamicExplosion(player.getX(),player.getY(),flammability,explosiveness,power,radius,Pal.darkFlame);
                            eb.setTitle("Command executed successfully.");
                            eb.setDescription("Exploded with: " + flammability + " Flammability,  " + explosiveness+" Explosiveness, "+power+" Power, and with a Radius of "+ radius + " near " + escapeCharacters(player.name) + ".");
                            ctx.channel.sendMessage(eb);
                        }
                    }
                }
            });

            handler.registerCommand(new RoleRestrictedCommand("killunits") {
                {
                    help = "<playerid|ip|name> <unit> Kills all units of the team of the specified player";
                    role = banRole;
                }
                public void run(Context ctx) {
                    EmbedBuilder eb = new EmbedBuilder();
                    String target = ctx.args[1];
                    String targetUnit = ctx.args[2];
                    UnitType desiredUnit = UnitTypes.dagger;
                    if(target.length() > 0 && targetUnit.length() > 0) {
                        try {
                            Field field = UnitTypes.class.getDeclaredField(targetUnit);
                            desiredUnit = (UnitType)field.get(null);
                        } catch (NoSuchFieldException | IllegalAccessException ignored) {}

                        Player player = findPlayer(target);
                        if(player!=null){
                            int amount = 0;
                            for(BaseUnit unit : Vars.unitGroup.all()) {
                                if(unit.getTeam() == player.getTeam()){
                                    if(unit.getType() == desiredUnit) {
                                        unit.kill();
                                        amount++;
                                    }
                                }
                            }
                            eb.setTitle("Command executed successfully.");
                            eb.setDescription("Killed " + amount + " " + targetUnit + "s on team " + player.getTeam());
                            ctx.channel.sendMessage(eb);
                        }
                    } else{
                        eb.setTitle("Command terminated");
                        eb.setDescription("Invalid arguments provided.");
                        eb.setColor(Pals.error);
                        ctx.channel.sendMessage(eb);
                    }
                }
            });

            handler.registerCommand(new RoleRestrictedCommand("setblock") {
                {
                    help = "<playerid|ip|name> <block> Create a block at the player's current location and on the player's current team.";
                    role = banRole;
                }
                public void run(Context ctx) {
                    String target = ctx.args[1];
                    String targetBlock = ctx.args[2];
                    Block desiredBlock = Blocks.copperWall;

                    try {
                        Field field = Blocks.class.getDeclaredField(targetBlock);
                        desiredBlock = (Block)field.get(null);
                    } catch (NoSuchFieldException | IllegalAccessException ignored) {}

                    EmbedBuilder eb = new EmbedBuilder();
                    Player player = findPlayer(target);

                    if(player!=null){
                        float x = player.getX();
                        float y = player.getY();
                        Tile tile = world.tileWorld(x, y);
                        tile.setNet(desiredBlock, player.getTeam(), 0);

                        eb.setTitle("Command executed successfully.");
                        eb.setDescription("Spawned " + desiredBlock.name + " on " + Utils.escapeCharacters(player.name) + "'s position.");
                        ctx.channel.sendMessage(eb);
                    } else{
                        eb.setTitle("Command terminated");
                        eb.setDescription("Invalid arguments provided.");
                        eb.setColor(Pals.error);
                        ctx.channel.sendMessage(eb);
                    }
                }
            });

            handler.registerCommand(new RoleRestrictedCommand("weaponmod") { // OH NO
                {
                    help = "<playerid|ip|name|all(oh no)> <bullet-type> <lifetime-modifier> <velocity-modifier> Mod the current weapon of a player.";
                    role = banRole;
                }
                public void run(Context ctx) {
                    EmbedBuilder eb = new EmbedBuilder();

                    String target = ctx.args[1];
                    String targetBullet = ctx.args[2];
                    float targetL = Float.parseFloat(ctx.args[3]);
                    float targetV = Float.parseFloat(ctx.args[4]);
                    BulletType desiredBullet = null;

                    if(target.length() > 0 && targetBullet.length() > 0) {
                        try {
                            Field field = Bullets.class.getDeclaredField(targetBullet);
                            desiredBullet = (BulletType)field.get(null);
                        } catch (NoSuchFieldException | IllegalAccessException ignored) {}

                        if(target.equals("all")){
                            for(Player p : playerGroup.all()){
                                TempPlayerData tdata = ioMain.playerDataGroup.get(p.uuid);
                                if(desiredBullet == null){
                                    tdata.bt = null;
                                } else{
                                    tdata.bt = desiredBullet;
                                    tdata.sclLifetime = targetL;
                                    tdata.sclVelocity = targetV;
                                }
                            }
                            eb.setTitle("Command executed");
                            eb.setDescription("Changed everyone's weapon mod. sorry. i dont know how to explain the rest");
                            ctx.channel.sendMessage(eb);
                        }

                        Player player = findPlayer(target);
                        if(player!=null){
                            TempPlayerData tdata = ioMain.playerDataGroup.get(player.uuid);
                            if(desiredBullet == null){
                                tdata.bt = null;
                                eb.setTitle("Command executed");
                                eb.setDescription("Reverted " + escapeCharacters(player.name) + "'s weapon to default.");
                                ctx.channel.sendMessage(eb);
                            } else{
                                tdata.bt = desiredBullet;
                                tdata.sclLifetime = targetL;
                                tdata.sclVelocity = targetV;
                                eb.setTitle("Command executed");
                                eb.setDescription("Modded " + escapeCharacters(player.name) + "'s weapon to " + targetBullet + " with " + targetL + "x lifetime modifier and " + targetV + "x velocity modifier.");
                                ctx.channel.sendMessage(eb);
                            }
                        }
                    } else{
                        eb.setTitle("Command terminated");
                        eb.setDescription("Invalid arguments provided.");
                        eb.setColor(Pals.error);
                        ctx.channel.sendMessage(eb);
                    }
                }
            });

        }

            /*handler.registerCommand(new Command("sendm"){ // use sendm to send embed messages when needed locally, disable for now
                public void run(Context ctx){
                    EmbedBuilder eb = new EmbedBuilder()
                            .setColor(Utils.Pals.info)
                            .setTitle("Support mindustry.io by donating, and receive custom ranks!")
                            .setUrl("https://donate.mindustry.io/")
                            .setDescription("By donating, you directly help me pay for the monthly server bills I receive for hosting 4 servers with **150+** concurrent players daily.")
                            .addField("VIP", "**VIP** is obtainable through __nitro boosting__ the server or __donating $1.59+__ to the server.", false)
                            .addField("__**MVP**__", "**MVP** is a more enchanced **vip** rank, obtainable only through __donating $3.39+__ to the server.", false)
                            .addField("Where do I get it?", "You can purchase **vip** & **mvp** ranks here: https://donate.mindustry.io", false)
                            .addField("\uD83E\uDD14 io is pay2win???", "Nope. All perks vips & mvp's gain are aesthetic items **or** items that indirectly help the team. Powerful commands that could give you an advantage are __disabled on pvp.__", true);
                    ctx.channel.sendMessage(eb);
                }
            });*/


        if(data.has("mapSubmissions_id")){
            TextChannel tc = ioMain.getTextChannel(ioMain.data.getString("mapSubmissions_id"));
            handler.registerCommand(new Command("submitmap") {
                {
                    help = "<.msav attachment> Submit a new map to be added into the server playlist in a .msav file format.";
                }
                public void run(Context ctx) {
                    EmbedBuilder eb = new EmbedBuilder();
                    Array<MessageAttachment> ml = new Array<>();
                    for (MessageAttachment ma : ctx.event.getMessageAttachments()) {
                        if (ma.getFileName().split("\\.", 2)[1].trim().equals("msav")) {
                            ml.add(ma);
                        }
                    }
                    if (ml.size != 1) {
                        eb.setTitle("Map upload terminated.");
                        eb.setColor(Pals.error);
                        eb.setDescription("You need to add one valid .msav file!");
                        ctx.channel.sendMessage(eb);
                        return;
                    } else if (Core.settings.getDataDirectory().child("maps").child(ml.get(0).getFileName()).exists()) {
                        eb.setTitle("Map upload terminated.");
                        eb.setColor(Pals.error);
                        eb.setDescription("There is already a map with this name on the server!");
                        ctx.channel.sendMessage(eb);
                        return;
                    }
                    CompletableFuture<byte[]> cf = ml.get(0).downloadAsByteArray();

                    try {
                        byte[] data = cf.get();
                        if (!SaveIO.isSaveValid(new DataInputStream(new InflaterInputStream(new ByteArrayInputStream(data))))) {
                            eb.setTitle("Map upload terminated.");
                            eb.setColor(Pals.error);
                            eb.setDescription("Map file corrupted or invalid.");
                            ctx.channel.sendMessage(eb);
                            return;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    eb.setTitle("Map upload completed.");
                    eb.setDescription(ml.get(0).getFileName() + " was successfully queued for review by moderators!");
                    ctx.channel.sendMessage(eb);
                    EmbedBuilder eb2 = new EmbedBuilder()
                            .setTitle("A map submission has been made for " + ioMain.serverName)
                            .setAuthor(ctx.author)
                            .setTimestampToNow()
                            .addField("Name", ml.get(0).getFileName())
                            .addField("URL", String.valueOf(ml.get(0).getUrl()));
                    tc.sendMessage(eb2);
                }
            });
        }
    }
}