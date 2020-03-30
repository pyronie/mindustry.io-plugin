package mindustry.plugin.commands;

import arc.Events;
import arc.util.CommandHandler;
import arc.util.Log;
import mindustry.content.Bullets;
import mindustry.content.Mechs;
import mindustry.content.UnitTypes;
import mindustry.entities.bullet.BulletType;
import mindustry.entities.traits.HealthTrait;
import mindustry.entities.type.BaseUnit;
import mindustry.entities.type.Player;
import mindustry.entities.type.Unit;
import mindustry.game.EventType;
import mindustry.game.Team;
import mindustry.gen.Call;
import mindustry.graphics.Pal;
import mindustry.maps.Map;
import mindustry.net.Administration;
import mindustry.net.Packets;
import mindustry.plugin.datas.PlayerData;
import mindustry.plugin.discord.Context;
import mindustry.plugin.utils.Funcs;
import mindustry.type.Mech;
import mindustry.type.UnitType;
import net.dv8tion.jda.api.EmbedBuilder;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.stream.IntStream;

import static mindustry.Vars.*;
import static mindustry.net.Administration.*;
import static mindustry.net.Packets.*;
import static mindustry.plugin.discord.Loader.prefix;
import static mindustry.plugin.discord.Loader.serverName;
import static mindustry.plugin.utils.Funcs.*;

public class ModeratorCommands {
    public ModeratorCommands(){
    }

    public void registerCommands(CommandHandler handler){
        handler.<Context>register("announce", "<text...>", "Display a message on top of the screen for all players for 10 seconds", (args, ctx) -> {
            Call.onInfoToast(args[0], 10);
            ctx.sendEmbed(true, ":round_pushpin: announcement sent successfully!", args[0]);
        });

        handler.<Context>register("event", "<ip> <port> [force?]", "Set the new event's ip & port. If force is set to true, all players will be forced to join immedietly.", (args, ctx) -> {
            eventIp = args[0];
            eventPort = Integer.parseInt(args[1]);
            boolean f = false;
            if(args.length >= 3 && Boolean.parseBoolean(args[2])) {
                f = true;
                playerGroup.all().forEach(player -> {
                    Call.onConnect(player.con, eventIp, eventPort);
                });
            }
            ctx.sendEmbed(true, ":crossed_swords: event ip set successfully!", args[0] + ":" + eventPort + (f ? "\nalso forced everyone to join" : ""));
        });

        handler.<Context>register("alert", "<player> <text...>", " " + serverName, (args, ctx) -> {
            Player player = findPlayer(args[0]);
            if(args[0].toLowerCase().equals("all")){
                Call.onInfoMessage(args[1]);
                ctx.sendEmbed(true, ":round_pushpin: alert to everyone sent successfully!", args[1]);
            }else{
                if(player != null){
                    Call.onInfoMessage(player.con, args[1]);
                    ctx.sendEmbed(true, ":round_pushpin: alert to " + escapeCharacters(player.name) + " sent successfully!", args[1]);
                }else{
                    ctx.sendEmbed(false, ":round_pushpin: can't find player " + args[1]);
                }
            }
        });

        handler.<Context>register("ban", "<player> <minutes> [reason...]", "Ban a player by the provided name, id or uuid (do offline bans using uuid)", (args, ctx) -> {
            Player player = findPlayer(args[0]);
            if(player!= null){
                PlayerData pd = getData(player.uuid);
                if(pd != null){
                    long until = Instant.now().getEpochSecond() + Integer.parseInt(args[1]) * 60;
                    pd.bannedUntil = until;
                    pd.banReason = (args.length >= 3 ? args[2] : "not specified") + "\n" + "[accent]Until: " + epochToString(until) + "\n[accent]Ban ID:[] " + player.uuid.substring(0, 4);
                    setData(player.uuid, pd);
                    ctx.sendEmbed(true, ":hammer: the ban hammer has been swung at " + escapeCharacters(player.name), "reason: *" + pd.banReason + "*\n");
                    player.con.kick(KickReason.banned);
                }else{
                    ctx.sendEmbed(false, ":interrobang: internal server error, please ping fuzz");
                }
            }else{
                PlayerData pd = getData(args[0]);
                if(pd != null){
                    long until = Instant.now().getEpochSecond() + Integer.parseInt(args[1]) * 60;
                    pd.bannedUntil = until;
                    pd.banReason = (args.length >= 3 ? args[2] : "not specified") + "\n" + "[accent]Until: " + epochToString(until) + "\n[accent]Ban ID:[] " + player.uuid.substring(0, 4);
                    setData(player.uuid, pd);
                    ctx.sendEmbed(true, ":hammer: the ban hammer has been swung at " + escapeCharacters(netServer.admins.getInfo(args[0]).lastName),"reason: *" + pd.banReason + "*");
                }else{
                    ctx.sendEmbed(false, ":hammer: that player or uuid cannot be found");
                }
            }
        });

        handler.<Context>register("kick", "<player>", "Kick a player from " + serverName, (args, ctx) -> {
            Player player = findPlayer(args[0]);
            if(player != null){
                player.con.kick(KickReason.kick);
                ctx.sendEmbed(true, ":football: kicked " + escapeCharacters(player.name) + " successfully!");
            }else{
                ctx.sendEmbed(false, ":round_pushpin: can't find player " + escapeCharacters(args[0]));
            }
        });

        handler.<Context>register("unban", "<uuid>", "Unban the specified player by uuid (works for votekicks as well)", (args, ctx) -> {
            PlayerData pd = getData(args[0]);
            if(pd!= null){
                PlayerInfo info = netServer.admins.getInfo(args[0]);
                info.lastKicked = 0;
                pd.bannedUntil = 0;
                setData(player.uuid, pd);
                ctx.sendEmbed(true, ":wrench: unbanned " + escapeCharacters(info.lastName) + " successfully!");
            }else{
                ctx.sendEmbed(false, ":wrench: that uuid doesn't exist in the database..");
            }
        });

        handler.<Context>register("lookup", "<uuid|name>", "Lookup the specified player by uuid or name (name search only works when player is online)", (args, ctx) -> {
            EmbedBuilder eb = new EmbedBuilder();
            Administration.PlayerInfo info;
            Player player = findPlayer(args[0]);
            if (player != null) {
                info = netServer.admins.getInfo(player.uuid);
            } else{
                info = netServer.admins.getInfo(args[0]);
            }
            eb.setColor(Pals.progress);
            eb.setTitle(":mag: " + escapeCharacters(info.lastName) + "'s lookup");
            eb.addField("UUID", info.id, false);
            eb.addField("Last used ip", info.lastIP, true);
            eb.addField("Times kicked", String.valueOf(info.timesKicked), true);

            StringBuilder s = new StringBuilder();
            s.append("**All used names: **\n");
            for (String name : info.names) {
                s.append(escapeCharacters(name)).append(" / ");
            }
            s.append("\n\n**All used IPs: **\n");
            for (String ip : info.ips) {
                s.append(escapeCharacters(ip)).append(" / ");
            }
            eb.setDescription(s.toString());
            ctx.channel.sendMessage(eb.build()).queue();
        });

        handler.<Context>register("mech", "<player> <mech>", "Change a players mech into the specified mech", (args, ctx) -> {
            Mech desiredMech = Mechs.alpha;
            try {
                Field field = Mechs.class.getDeclaredField(args[1]);
                desiredMech = (Mech)field.get(null);
            } catch (NoSuchFieldException | IllegalAccessException ignored) {
                ctx.sendEmbed(false, ":robot: that mech doesn't exist");
                return;
            }
            Player player = findPlayer(args[0]);
            if(player != null){
                player.mech = desiredMech;
                ctx.sendEmbed(true, ":robot: changed " + escapeCharacters(player.name) + "'s mech to " + desiredMech.name);
            }else if(args[0].toLowerCase().equals("all")){
                for(Player p : playerGroup.all()){ p.mech = desiredMech; }
                ctx.sendEmbed(true, ":robot: changed everyone's mech to " + desiredMech.name);
            }else{
                ctx.sendEmbed(false, ":robot: can't find " + escapeCharacters(args[0]));
            }
        });

        handler.<Context>register("team", "<player> <teamid>", "Change a players team into the specified team id", (args, ctx) -> {
            int teamid;
            try{
                teamid = Integer.parseInt(args[1]);
            }catch (Exception e){ ctx.sendEmbed(false, ":triangular_flag_on_post: error parsing team id number"); return;}

            Player player = findPlayer(args[0]);
            if(player != null){
                player.setTeam(Team.get(teamid));
                ctx.sendEmbed(true, ":triangular_flag_on_post: changed " + escapeCharacters(player.name) + "'s team to " + Team.get(teamid).name);
            }else if(args[0].toLowerCase().equals("all")){
                for(Player p : playerGroup.all()){ p.setTeam(Team.get(teamid)); }
                ctx.sendEmbed(true, ":triangular_flag_on_post: changed everyone's team to " + Team.get(teamid).name);
            }else{
                ctx.sendEmbed(false, ":triangular_flag_on_post: can't find " + escapeCharacters(args[0]));
            }
        });

        handler.<Context>register("motd", "<message...>", "Change the welcome message popup when a new player joins Set to 'none' if you want to disable motd", (args, ctx) -> {
            if(args[0].toLowerCase().equals("none")){
                welcomeMessage = "";
                ctx.sendEmbed(true, ":newspaper: disabled welcome message successfully!");
            }else{
                welcomeMessage = args[0];
                ctx.sendEmbed(true, ":newspaper: changed welcome message successfully!", args[0]);
            }
        });

        handler.<Context>register("statmessage", "<message...>", "Change the stat message popup when a player uses the /info command", (args, ctx) -> {
            if(args[0].toLowerCase().equals("none")){
                statMessage = "";
                ctx.sendEmbed(true, ":newspaper: disabled stat message successfully!");
            }else{
                statMessage = args[0];
                ctx.sendEmbed(true, ":newspaper: changed stat message successfully!", args[0]);
            }
        });

        handler.<Context>register("spawn", "<player> <unit> <amount>", "Spawn a specified amount of units near the player's position.", (args, ctx) -> {
            int amt;
            try{
                amt = Integer.parseInt(args[1]);
            }catch (Exception e){ ctx.sendEmbed(false, ":robot: error parsing amount number"); return;}

            UnitType desiredUnitType = UnitTypes.dagger;
            try {
                Field field = UnitTypes.class.getDeclaredField(args[1]);
                desiredUnitType = (UnitType) field.get(null);
            } catch (NoSuchFieldException | IllegalAccessException ignored) {
                ctx.sendEmbed(false, ":robot: that unit doesn't exist");
                return;
            }
            Player player = findPlayer(args[0]);
            if(player != null){
                UnitType finalDesiredUnitType = desiredUnitType;
                IntStream.range(0, amt).forEach(i -> {
                    BaseUnit unit = finalDesiredUnitType.create(player.getTeam());
                    unit.set(player.getX(), player.getY());
                    unit.add();
                });
            }else{
                ctx.sendEmbed(false, ":robot: can't find " + escapeCharacters(args[0]));
            }
        });

        handler.<Context>register("kill", "<player|unit>", "Kill the specified player or all specified units on the map.", (args, ctx) -> {
            UnitType desiredUnitType = UnitTypes.dagger;
            try {
                Field field = UnitTypes.class.getDeclaredField(args[0]);
                desiredUnitType = (UnitType) field.get(null);
                int amt = 0;
                for(Unit unit : unitGroup.all()){
                    if(unit.getTypeID() == desiredUnitType.typeID){ unit.kill(); amt++; }
                }
                ctx.sendEmbed(true, ":knife: killed " + amt + " " + args[0] + "s");
            } catch (NoSuchFieldException | IllegalAccessException ignored) {
                Player player = findPlayer(args[0]);
                if(player != null){
                    player.kill();
                    ctx.sendEmbed(true, ":knife: killed " + escapeCharacters(player.name));
                }else if(args[0].toLowerCase().equals("all")){
                    playerGroup.all().forEach(HealthTrait::kill);
                    ctx.sendEmbed(true, ":knife: killed everyone, muhahaha");
                }else{
                    ctx.sendEmbed(false, ":knife: can't find " + escapeCharacters(args[0]));
                }
            }
        });

        handler.<Context>register("weapon", "<player> <bullet> [lifetime] [velocity]", "Modify the specified players weapon with the provided parameters", (args, ctx) -> {
            BulletType desiredBulletType;
            float life = 1f;
            float vel = 1f;
            if(args.length > 2){
                try{
                    life = Float.parseFloat(args[3]);
                }catch (Exception e){ ctx.sendEmbed(false, ":gun: error parsing lifetime number"); return;}
            }
            if(args.length > 3){
                try{
                    vel = Float.parseFloat(args[4]);
                }catch (Exception e){ ctx.sendEmbed(false, ":gun: error parsing velocity number"); return;}
            }
            try {
                Field field = Bullets.class.getDeclaredField(args[1]);
                desiredBulletType = (BulletType) field.get(null);
            } catch (NoSuchFieldException | IllegalAccessException ignored) {
                ctx.sendEmbed(false, ":gun: invalid bullet type");
                return;
            }
            //todo: finish
        });
    }
}