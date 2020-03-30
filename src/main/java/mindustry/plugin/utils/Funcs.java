package mindustry.plugin.utils;

import arc.Core;
import arc.Events;
import arc.struct.Array;
import arc.util.Strings;
import mindustry.content.Blocks;
import mindustry.entities.type.Player;
import mindustry.entities.type.TileEntity;
import mindustry.game.EventType;
import mindustry.game.Team;
import mindustry.maps.Map;
import mindustry.maps.Maps;
import mindustry.plugin.datas.MapData;
import mindustry.plugin.datas.PlayerData;
import mindustry.plugin.discord.Loader;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.storage.CoreBlock;
import net.dv8tion.jda.api.entities.User;
import redis.clients.jedis.Jedis;

import java.awt.*;
import java.lang.reflect.Field;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static mindustry.Vars.*;
import static mindustry.plugin.discord.Loader.*;
import static mindustry.plugin.ioMain.*;

public class Funcs {
    public static int chatMessageMaxSize = 256;
    public static String assets = "iocontent/";
    static String mapSaveKey = "bXn94MAP";
    public static String welcomeMessage = "";
    public static String statMessage = "";
    public static String noPermissionMessage = "[accent]You don't have permissions to execute this command!\nObtain the donator rank here: http://donate.mindustry.io";

    // wheter ip verification is in place (detect vpns, disallow their build rights)
    public static Boolean verification = true;

    public static String promotionMessage =
            "[sky]%player%, you have been promoted to [sky]<active>[]!\n" +
            "[#4287f5]You reached a playtime of - %playtime% minutes! That's 10+ hours!\n" +
            "[#f54263]You played a total of %games% games!\n" +
            "[#9342f5]You built a total of %buildings% buildings!\n" +
            "[sky]Thank you for participating and enjoy your time on [orange]<[white]io[orange]>[sky]!\n"+
            "[scarlet]Please rejoin for the change to take effect.";

    public static String verificationMessage = "[scarlet]Your IP was flagged as a VPN.\n" +
            "\n" +
            "[sky]Please join our discord:\n" +
            "http://discord.mindustry.io\n" +
            "[#7a7a7a]verify your account in #verifications";

    public static HashMap<Integer, Rank> rankNames = new HashMap<>();
    static HashMap<String, Integer> rankRoles = new HashMap<>();
    public static Array<String> bannedNames = new Array<>();
    public static Array<String> onScreenMessages = new Array<>();
    public static String eventIp = "";
    public static int eventPort = 6567;

    public static class Rank{
        public String tag = "";
        public String name = "";

        Rank(String t, String n){
            this.tag = t;
            this.name = n;
        }
    }

    public static void init(){
        rankNames.put(0, new Rank("[#7d7d7d]<none>[]", "guest"));
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
        bannedNames.add("tuttop");
        bannedNames.add("IgruhaOrg");

        activeRequirements.bannedBlocks.add(Blocks.conveyor);
        activeRequirements.bannedBlocks.add(Blocks.titaniumConveyor);
        activeRequirements.bannedBlocks.add(Blocks.junction);
        activeRequirements.bannedBlocks.add(Blocks.router);

        statMessage = Core.settings.getString("statMessage");
        welcomeMessage = Core.settings.getString("welcomeMessage");
    }

    public static class Pals {
        public static Color error = new Color(255, 60, 60);
        public static Color success = new Color(60, 255, 100);
        public static Color progress = new Color(252, 243, 120);
    }

    public static class activeRequirements {
        public static Array<Block> bannedBlocks = new Array<>();
        public static int playtime = 60 * 10;
        public static int buildingsBuilt = 1000 * 10;
        public static int gamesPlayed = 10;
    }

    public static String escapeCharacters(String string){
        return escapeColorCodes(string.replaceAll("`", "").replaceAll("@", ""));
    }

    public static String escapeColorCodes(String string){
        return Strings.stripColors(string);
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

            if (player.con.address.equals(identifier.replaceAll(" ", "")) || String.valueOf(player.id).equals(identifier.replaceAll(" ", "")) || player.uuid.equals(identifier.replaceAll(" ", "")) || escapeColorCodes(player.name.toLowerCase().replaceAll(" ", "")).replaceAll("<.*?>", "").startsWith(identifier.toLowerCase().replaceAll(" ", ""))) {
                found = player;
            }
        }
        return found;
    }

    public static String formatMessage(Player player, String message){
        try {
            message = message.replaceAll("%player%", escapeCharacters(player.name));
            message = message.replaceAll("%map%", world.getMap().name());
            message = message.replaceAll("%wave%", String.valueOf(state.wave));
            PlayerData pd = getData(player.uuid);
            if (pd != null) {

            }
        }catch(Exception ignore){};
        return message;
    }


    // playerdata
    public static PlayerData getData(String uuid) {
        try(Jedis jedis = pool.getResource()) {
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

    public static void changeMap(Map found){
        Class<Maps> mapsClass = Maps.class;
        Field mapsField;
        try {
            mapsField = mapsClass.getDeclaredField("maps");
        } catch (NoSuchFieldException ex) {
            throw new RuntimeException("Could not find field 'maps' of class 'mindustry.maps.Maps'");
        }
        mapsField.setAccessible(true);
        Field mapsListField = mapsField;

        Array<Map> mapsList;
        try {
            mapsList = (Array<Map>)mapsListField.get(maps);
        } catch (IllegalAccessException ex) {
            throw new RuntimeException("unreachable");
        }

        Array<Map> tempMapsList = mapsList.removeAll(map -> !map.custom || map != found);

        try {
            mapsListField.set(maps, tempMapsList);
        } catch (IllegalAccessException ex) {
            throw new RuntimeException("unreachable");
        }

        Events.fire(new EventType.GameOverEvent(Team.crux));

        try {
            mapsListField.set(maps, mapsList);
        } catch (IllegalAccessException ex) {
            throw new RuntimeException("unreachable");
        }
        maps.reload();
    }

    public static CoreBlock.CoreEntity getCore(Team team){
        Tile[][] tiles = world.getTiles();
        for (int x = 0; x < tiles.length; ++x) {
            for(int y = 0; y < tiles[0].length; ++y) {
                if (tiles[x][y] != null && tiles[x][y].entity != null) {
                    TileEntity ent = tiles[x][y].ent();
                    if (ent instanceof CoreBlock.CoreEntity) {
                        if(ent.getTeam() == team){
                            return (CoreBlock.CoreEntity) ent;
                        }
                    }
                }
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
