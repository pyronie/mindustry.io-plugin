package mindustry.plugin.utils;

import arc.Core;
import arc.Events;
import arc.graphics.Pixmap;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.Strings;
import mindustry.game.EventType;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.gen.Call;
import mindustry.gen.Groups;
import mindustry.gen.Player;
import mindustry.io.MapIO;
import mindustry.plugin.datas.PlayerData;
import mindustry.plugin.discord.Loader;
import mindustry.server.ServerControl;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.storage.CoreBlock;
import redis.clients.jedis.Jedis;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static mindustry.Vars.*;
import static mindustry.plugin.discord.Loader.*;
import static mindustry.plugin.ioMain.*;
import static mindustry.content.Blocks.*;

public class Funcs {
    public static int chatMessageMaxSize = 256;
    public static String assets = "iocontent/";
    public static String welcomeMessage = "";
    public static String statMessage = "";
    public static String promotionMessage = "[sky]%player%, you have been promoted to [sky]<active>[]!\n" + "[#4287f5]You reached a playtime of - %playtime% minutes! That's 10+ hours!\n" + "[#f54263]You played a total of %games% games!\n" + "[#9342f5]You built a total of %buildings% buildings!\n" + "[sky]Thank you for participating and enjoy your time on [orange]<[white]io[orange]>[sky]!\n" + "[scarlet]Please rejoin for the change to take effect.";


    public static HashMap<Integer, Rank> rankNames = new HashMap<>();
    public static ArrayList<String> onScreenMessages = new ArrayList<>();
    public static ArrayList<String> bannedNames = new ArrayList<>();
    public static String eventIp = "";
    public static int eventPort = 6567;

    public static class Rank{
        public String tag;
        public String name;
        public String rawTag;

        Rank(String t, String rt, String n){
            this.tag = t;
            this.rawTag = rt;
            this.name = n;
        }
    }

    public static void init(){
        rankNames.put(0, new Rank("[#7d7d7d]<none>[]", "[NONE]", "none"));
        rankNames.put(1, new Rank("[accent]<[white][accent]>[]", "", "Active"));
        rankNames.put(2, new Rank("[accent]<[white][accent]>[]", "", "Veteran"));
        rankNames.put(3, new Rank("[accent]<[white][accent]>[]","", "Contributor"));
        rankNames.put(4, new Rank("[accent]<[white][accent]>[]","", "Apprentice Moderator"));
        rankNames.put(5, new Rank("[accent]<[white][accent]>[]", "","Moderator"));
        rankNames.put(6, new Rank("[accent]<[white]\uE82C[accent]>[]", "\uE82C","Admin"));

        bannedNames.add("IGGGAMES");
        bannedNames.add("CODEX");
        bannedNames.add("VALVE");
        bannedNames.add("tuttop");
        bannedNames.add("Volas Y0uKn0w1sR34Lp");
        bannedNames.add("IgruhaOrg");
        bannedNames.add("андрей");
        bannedNames.add("�");
        bannedNames.add("nigger");
        bannedNames.add("sieg heil");

        activeRequirements.bannedBlocks.addAll(router, conveyor, titaniumConveyor, armoredConveyor, junction, sorter, invertedSorter, overflowGate, underflowGate, liquidRouter, conduit, pulseConduit, platedConduit, liquidJunction, copperWall);

        statMessage = Core.settings.getString("statMessage");
        welcomeMessage = Core.settings.getString("welcomeMessage");
    }

    public static class Pals {
        public static Color error = new Color(255, 60, 60);
        public static Color success = new Color(60, 255, 100);
        public static Color progress = new Color(252, 243, 120);
        public static Color freeze = new Color(43, 151, 232);
    }

    public static class activeRequirements{

        public static Seq<Block> bannedBlocks = new Seq<Block>();
        public static int playtime = 75 * 10;
        public static int buildingsBuilt = 1500 * 10;
        public static int gamesPlayed = 10;
    }

    public static String escapeCharacters(String string){
        String escaped = escapeColorCodes(string.replaceAll("`", "").replaceAll("@", ""));
        if(escaped.contains(" ")) {
            boolean containsTag = false;
            for (Rank r : rankNames.values()) {
                if (escaped.contains(r.rawTag))
                    containsTag = true;
            }
            if(containsTag)
                escaped = escaped.substring(escaped.indexOf(" ")+1);
        }

        return escaped;
    }

    public static String escapeColorCodes(String string){
        return Strings.stripColors(string);
    }

    public static mindustry.maps.Map getMapBySelector(String query) {
        mindustry.maps.Map found = null;
        try {
            // try by number
            found = maps.customMaps().get(Integer.parseInt(query));
        } catch (Exception e) {
            // try by name
            for (mindustry.maps.Map m : maps.customMaps()) {
                if (m.name().replaceAll(" ", "").toLowerCase().contains(query.toLowerCase().replaceAll(" ", ""))) {
                    found = m;
                    break;
                }
            }
        }
        return found;
    }

    public static Player findPlayer(String identifier){
        for (Player player : Groups.player) {
            if(player == null) return null;
            if(player.uuid() == null) return null;
            if(player.con == null) return null;
            if(player.con.address == null) return null;

            if (player.con.address.equals(identifier.replaceAll(" ", "")) || String.valueOf(player.id).equals(identifier.replaceAll(" ", "")) || player.uuid().equals(identifier.replaceAll(" ", "")) || escapeColorCodes(player.name.toLowerCase().replaceAll(" ", "")).replaceAll("<.*?>", "").startsWith(identifier.toLowerCase().replaceAll(" ", "")) || escapeColorCodes(player.name.toLowerCase().substring(player.name.indexOf(" ")+1)).trim().replaceAll("<.*?>", "").startsWith(identifier.toLowerCase().replaceAll(" ", ""))) {
                return player;
            }
        }
        return null;
    }

    public static String formatMessage(Player player, String message){
        try {
            message = message.replaceAll("%player%", escapeCharacters(player.name));
            message = message.replaceAll("%map%", state.map.name());
            message = message.replaceAll("%wave%", String.valueOf(state.wave));
            PlayerData pd = Database.getData(player.uuid());
            if (pd != null) {
                message = message.replaceAll("%rank%", rankNames.get(pd.rank).name);
                message = message.replaceAll("%playtime%", String.valueOf(pd.playTime));
                message = message.replaceAll("%games%", String.valueOf(pd.gamesPlayed));
                message = message.replaceAll("%buildings%", String.valueOf(pd.buildingsBuilt));
                message = message.replaceAll("%discord%", (pd.discord_id == "" ? "[gray]unlinked[]" : Loader.getDiscordUser(pd.discord_id).getEffectiveName()));
            }
        }catch(Exception ignore){};
        return message;
    }


    // playerdata
    public static PlayerData getJedisData(String uuid) {
        try(Jedis jedis = pool.getResource()) {
            String json = jedis.get(uuid);
            if(json == null) return null;
            try {
                return gson.fromJson(json, PlayerData.class);
            } catch(Exception e){
                return null;
            }
        }
    }

    public static void removeJedisData(String uuid){
        CompletableFuture.runAsync(() -> {
            try (Jedis jedis = pool.getResource()) {
                try {
                    jedis.set(uuid, null);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public static void setJedisData(String uuid, PlayerData pd) {
        CompletableFuture.runAsync(() -> {
            try (Jedis jedis = pool.getResource()) {
                try {
                    String json = gson.toJson(pd);
                    jedis.set(uuid, json);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public static void SaveDatabase(String uuid){
        PlayerData buffer = tempPlayerDatas.get(uuid).buffer;
        Database.updateDataWithBuffer(uuid, buffer);
    }

    public static void CheckPromotion(Player p){
        PlayerData pd = Database.getData(p.uuid());
        if(pd == null) return;

        if(pd.rank == 0 && pd.playTime >= activeRequirements.playtime && pd.buildingsBuilt >= activeRequirements.buildingsBuilt && pd.gamesPlayed >= activeRequirements.gamesPlayed){
            Call.infoMessage(p.con, Funcs.formatMessage(p, promotionMessage));
            if(pd.rank == 0){
                pd.rank = 1;
                Database.updateData(p.uuid(), pd);
            }
        }
    }

    public static void SaveDatabase(){
        for(Player player : Groups.player){
            String uuid = player.uuid();
            PlayerData buffer = tempPlayerDatas.get(uuid).buffer;
            Database.updateDataWithBuffer(uuid, buffer);
            tempPlayerDatas.get(uuid).buffer = new PlayerData();
        }
    }

    public static void changeMap(mindustry.maps.Map found){
        maps.setMapProvider((mode, prev) -> found);
        Events.fire(new EventType.GameOverEvent(Team.crux));
        maps.setMapProvider(null);
    }

    public static void parseMap(mindustry.maps.Map map){
        try {
            Pixmap prv = MapIO.generatePreview(map);
            BufferedImage img = new BufferedImage(map.width, map.height, BufferedImage.TYPE_INT_ARGB);
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public static Building getCore(Team team){
        for(Tile t : world.tiles){
            if (t.build != null && t.build.block instanceof CoreBlock && t.build.team == team){
                return t.build;
            }
        }
        return null;
    }

    public static String epochToString(long epoch){
        Date date = new Date(epoch * 1000L);
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        format.setTimeZone(TimeZone.getTimeZone("Etc/UTC"));
        return format.format(date) + " UTC";
    }

    public static String getKeyByValue(HashMap<String, Integer> map, Integer value) {
        for (java.util.Map.Entry<String, Integer> entry : map.entrySet()) {
            if (value.equals(entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
    }
}
