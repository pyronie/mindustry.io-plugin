package mindustry.plugin.utils;

import arc.Core;
import arc.Events;
import arc.util.Log;
import arc.util.Strings;
import mindustry.content.Blocks;
import mindustry.game.EventType;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.gen.Groups;
import mindustry.gen.Player;
import mindustry.plugin.datas.PlayerData;
import mindustry.server.ServerControl;
import mindustry.world.Tile;
import mindustry.world.blocks.storage.CoreBlock;
import redis.clients.jedis.Jedis;

import java.awt.*;
import java.lang.reflect.Field;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static mindustry.Vars.*;
import static mindustry.plugin.discord.Loader.*;
import static mindustry.plugin.ioMain.*;

public class Funcs {
    public static int chatMessageMaxSize = 256;
    public static String assets = "iocontent/";
    public static String welcomeMessage = "";
    public static String statMessage = "";

    public static HashMap<Integer, Rank> rankNames = new HashMap<>();
    public static ArrayList<String> onScreenMessages = new ArrayList<>();
    public static String eventIp = "";
    public static int eventPort = 6567;

    public static class Rank{
        public String tag;
        public String name;

        Rank(String t, String n){
            this.tag = t;
            this.name = n;
        }
    }

    public static void init(){
        rankNames.put(0, new Rank("", "[lightgray]guest[]"));
        rankNames.put(1, new Rank("\uE809 ", "[#45a8ff]member[]"));
        rankNames.put(2, new Rank("\uE828 ", "[#ff6745]moderator[]"));

        statMessage = Core.settings.getString("statMessage");
        welcomeMessage = Core.settings.getString("welcomeMessage");
    }

    public static class Pals {
        public static Color error = new Color(255, 60, 60);
        public static Color success = new Color(60, 255, 100);
        public static Color progress = new Color(252, 243, 120);
    }

    public static String escapeCharacters(String string){
        return escapeColorCodes(string.replaceAll("`", "").replaceAll("@", ""));
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
        Player found = null;
        for (Player player : Groups.player) {
            if(player == null) return null;
            if(player.uuid() == null) return null;
            if(player.con == null) return null;
            if(player.con.address == null) return null;

            if (player.con.address.equals(identifier.replaceAll(" ", "")) || String.valueOf(player.id).equals(identifier.replaceAll(" ", "")) || player.uuid().equals(identifier.replaceAll(" ", "")) || escapeColorCodes(player.name.toLowerCase().replaceAll(" ", "")).replaceAll("<.*?>", "").startsWith(identifier.toLowerCase().replaceAll(" ", ""))) {
                found = player;
            }
        }
        return found;
    }

    public static String formatMessage(Player player, String message){
        try {
            message = message.replaceAll("%player%", escapeCharacters(player.name));
            message = message.replaceAll("%map%", state.map.name());
            message = message.replaceAll("%wave%", String.valueOf(state.wave));
            PlayerData pd = playerDataHashMap.get(player.uuid());
            if (pd != null) {
                message = message.replaceAll("%rank%", rankNames.get(pd.role).name);
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
                setJedisData(uuid, new PlayerData());
                return null;
            }
        }
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

    public static void changeMap(mindustry.maps.Map found){
        try {
            Field f = ServerControl.class.getDeclaredField("nextMapOverride");
            f.setAccessible(true);
            f.set(ServerControl.class, found);
            Log.info("next map override set: " + f.get(ServerControl.class));
        }catch(Exception e){

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
