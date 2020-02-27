package mindustry.plugin;

import arc.Events;
import arc.struct.Array;
import mindustry.content.Blocks;
import mindustry.entities.type.Player;
import mindustry.game.EventType;
import mindustry.game.Team;
import mindustry.maps.Map;
import mindustry.world.Block;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;
import redis.clients.jedis.exceptions.JedisException;

import java.awt.*;
import java.util.HashMap;

import static mindustry.Vars.*;
import static mindustry.plugin.ioMain.*;

public class Utils {
    public static int chatMessageMaxSize = 256;
    static String welcomeMessage = "";
    static String noPermissionMessage = "[accent]You don't have permissions to execute this command!\nObtain the donator rank here: http://donate.mindustry.io";
    static String statMessage = "";

    // wheter ip verification is in place (detect vpns, disallow their build rights)
    static Boolean verification = true;

    static String promotionMessage =
            "[sky]%player%, you have been promoted to [sky]<active>[]!\n" +
            "[#4287f5]You reached a playtime of - %playtime% minutes! That's 10+ hours!\n" +
            "[#f54263]You played a total of %games% games!\n" +
            "[#9342f5]You built a total of %buildings%!\n" +
            "[sky]Thank you for participating and enjoy your time on [orange]<[white]io[orange]>[sky]!\n"+
            "[scarlet]Please rejoin for the change to take effect.";

    static String verificationMessage = "[scarlet]Your IP was flagged as a VPN.\n" +
            "\n" +
            "[sky]Please join our discord:\n" +
            "http://discord.mindustry.io\n" +
            "[#7a7a7a]request manual verification in #verification-requests";

    static HashMap<Integer, Rank> rankNames = new HashMap<>();
    static HashMap<String, Integer> rankRoles = new HashMap<>();
    static Array<String> bannedNames = new Array<>();

    public static class Rank{
        public String tag = "";
        public String name = "";

        Rank(String t, String n){
            this.tag = t;
            this.name = n;
        }
    }

    public static void init(){
        rankNames.put(0, new Rank("[#7d7d7d]<none>[]", "none"));
        rankNames.put(1, new Rank("[accent]<[white]\uE810[accent]>[]", "active"));
        rankNames.put(2, new Rank("[accent]<[white]\uE809[accent]>[]", "regular"));
        rankNames.put(3, new Rank("[accent]<[white]\uE84E[accent]>[]", "donator"));
        rankNames.put(4, new Rank("[accent]<[white]\uE84F[accent]>[]", "moderator"));
        rankNames.put(5, new Rank("[accent]<[white]\uE828[accent]>[]", "admin"));

        rankRoles.put("627985513600516109", 1);
        rankRoles.put("636968410441318430", 2);
        rankRoles.put("674778262857187347", 3);
        rankRoles.put("624959361789329410", 4);

        bannedNames.add("IGGGAMES");
        bannedNames.add("CODEX");
        bannedNames.add("VALVE");

        activeRequirements.bannedBlocks.add(Blocks.conveyor);
        activeRequirements.bannedBlocks.add(Blocks.titaniumConveyor);
        activeRequirements.bannedBlocks.add(Blocks.junction);
        activeRequirements.bannedBlocks.add(Blocks.router);
    }

    public static class Pals {
        public static Color warning = (Color.getHSBColor(5, 85, 95));
        public static Color info = (Color.getHSBColor(45, 85, 95));
        public static Color error = (Color.getHSBColor(3, 78, 91));
    }

    public static class activeRequirements {
        public static Array<Block> bannedBlocks = new Array<>();
        public static int playtime = 60 * 10;
        public static int buildingsBuilt = 1000 * 10;
        public static int gamesPlayed = 1 * 10;
    }

    public static String escapeCharacters(String string){
        return escapeColorCodes(string.replaceAll("`", "").replaceAll("@", ""));
    }

    public static String escapeColorCodes(String string){
        return string.replaceAll("\\[(.*?)\\]", "");
    }

    public static Map getMapBySelector(String query) {
        Map found = null;
        try {
            // try by number
            found = maps.customMaps().get(Integer.parseInt(query));
        } catch (Exception e) {
            // try by name
            for (Map m : maps.customMaps()) {
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
        for (Player player : playerGroup.all()) {
            if(player == null) return null;
            if(player.uuid == null) return null;
            if(player.con == null) return null;
            if(player.con.address == null) return null;

            if (player.con.address.equals(identifier.replaceAll(" ", "")) || String.valueOf(player.id).equals(identifier.replaceAll(" ", "")) || player.uuid.equals(identifier.replaceAll(" ", "")) || escapeColorCodes(player.name.toLowerCase().replaceAll(" ", "")).replaceAll("<.*?>", "").contains(identifier.toLowerCase().replaceAll(" ", ""))) {
                found = player;
            }
        }
        return found;
    }

    public static String formatMessage(Player player, String message){
        message = message.replaceAll("%player%", escapeCharacters(player.name));
        message = message.replaceAll("%map%", world.getMap().name());
        message = message.replaceAll("%wave%", String.valueOf(state.wave));
        PlayerData pd = getData(player.uuid);
        if(pd != null) {
            message = message.replaceAll("%playtime%", String.valueOf(pd.playTime));
            message = message.replaceAll("%games%", String.valueOf(pd.gamesPlayed));
            message = message.replaceAll("%buildings%", String.valueOf(pd.buildingsBuilt));
            message = message.replaceAll("%rank%", rankNames.get(pd.rank).tag + " " + escapeColorCodes(rankNames.get(pd.rank).name));
        }
        return message;
    }

    public static PlayerData getData(String uuid) {
        try(Jedis jedis = ioMain.pool.getResource()) {
            String json = jedis.get(uuid);
            if(json == null) return null;

            try {
                return gson.fromJson(json, PlayerData.class);
            } catch(Exception e){
                e.printStackTrace();
                return null;
            }
        }
    }

    public static void setData(String uuid, PlayerData pd) {
        try(Jedis jedis = ioMain.pool.getResource()) {
            try {
                String json = gson.toJson(pd);
                jedis.set(uuid, json);
            } catch(Exception e){
                e.printStackTrace();
            }
        }
    }

}
