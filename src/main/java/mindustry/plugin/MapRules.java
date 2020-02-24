package mindustry.plugin;

import arc.util.Log;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.entities.type.Player;
import mindustry.entities.type.TileEntity;
import mindustry.gen.Call;
import mindustry.maps.Map;
import mindustry.net.Administration;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.storage.CoreBlock;

import static mindustry.plugin.Utils.*;

public class MapRules {
    public static class Maps{
        public static String minefield = "Minefield"; // pvp map
    }


    public static void onMapLoad(){
        // TODO: figure out a way to spawn all players quick

        Vars.netServer.admins.addActionFilter(action -> {
            Player player = action.player;
            if (player == null) return true;

            String uuid = player.uuid;
            if (uuid == null) return true;

            PlayerData pd = getData(uuid);
            if (pd == null) return true;

            // disable checks for admins
            if (player.isAdmin) return true;

            if (!pd.verified) {
                Call.setHudTextReliable(player.con, "[scarlet]Your IP was flagged as a VPN, please join http://discord.mindustry.io and request manual verification.");
                player.sendMessage("[#7a7a7a]Cannot build while flagged.");
                return false;
            }

            return action.type != Administration.ActionType.rotate;
        });

        // display map description on core tiles for the first minute
        Tile[][] tiles = Vars.world.getTiles();
        for (int x = 0; x < tiles.length; ++x) {
            for(int y = 0; y < tiles[0].length; ++y) {
                if (tiles[x][y] != null && tiles[x][y].entity != null) {
                    TileEntity ent = tiles[x][y].ent();
                    if (ent instanceof CoreBlock.CoreEntity) {
                        Map map = Vars.world.getMap();
                        Call.onLabel(map.description(), 60f, ent.x, ent.y);
                        Call.onInfoToast("Playing [accent]" + escapeColorCodes(map.name()) + "[] by[accent] " + map.author(), 10f); // credit map makers
                    }
                }
            }
        }
    }

    public static void run(){
        onMapLoad();
        Map map = Vars.world.getMap();
        Log.info(map.name());
        if (map.name().equals(Maps.minefield)) {
            Log.info("[MapRules]: Minefield action trigerred.");
            Call.sendMessage("[scarlet]Preparing minefield map, please wait.");
            Tile[][] tiles = Vars.world.getTiles();
            for (int x = 0; x < tiles.length; ++x) {
                for(int y = 0; y < tiles[0].length; ++y) {
                    if (tiles[x][y] != null && tiles[x][y].entity != null) {
                        Block block = tiles[x][y].block();
                        if (block != null) {
                            if(block == Blocks.shockMine){
                                Call.onTileDamage(tiles[x][y], 30f); // damage mines 30hp, leaving them with 10hp
                            }
                        }
                    }
                }
            }
        }
    }
}
