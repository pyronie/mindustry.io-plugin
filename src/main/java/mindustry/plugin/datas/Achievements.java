package mindustry.plugin.datas;

import arc.struct.Array;
import arc.util.Timer;
import mindustry.content.Blocks;
import mindustry.content.Fx;
import mindustry.content.Items;
import mindustry.entities.type.Player;
import mindustry.entities.type.TileEntity;
import mindustry.game.EventType;
import mindustry.gen.Call;
import mindustry.graphics.Pal;

import static mindustry.Vars.*;
import static mindustry.plugin.ioMain.playerDataHashMap;

public class Achievements {
    public Array<Achievement> all = new Array<>();

    public static Achievement wave1000;
    public static Achievement chernobyl;
    public static Achievement kamikaze;
    public static Achievement heresy;
    public static Achievement ignition;
    public static Achievement nostalgia;
    public static Achievement shrooms;
    public static Achievement shrooooms;
    public static Achievement snek;
    public static Achievement mmo;
    public static Achievement fuzz;

    public Achievements() {
    }

    public void load(){

        wave1000 = new Achievement(1,"5 hours later", "Reach wave 1000"){
            int limit = 5;
            @Override
            public void onWave(){
                int wave = state.wave;
                for(Player p : playerGroup.all()){
                    PlayerData pd = playerDataHashMap.get(p.uuid);
                    if(pd != null){
                        if(!pd.achievements.containsKey(id)) pd.achievements.put(id, 0f);
                        float progress = pd.achievements.get(id);
                        if(progress < 100){

                            progress = (wave / limit) * 100;
                            if(progress >= 100){
                                progress = 100;
                                displayCompletion(p);
                            }
                            pd.achievements.put(id, progress);
                            playerDataHashMap.put(p.uuid, pd);
                        }
                    }
                }
            }
        };
        all.add(wave1000);

        chernobyl = new Achievement(2,"Chernobyl", "Put thorium in a reactor without cooling"){
            @Override
            public void onItemDeposit(EventType.DepositEvent event){
                if(event.player == null) return;
                String uuid = event.player.uuid;
                if(event.tile.block() == Blocks.thoriumReactor){
                    TileEntity entity = event.tile.entity;
                    if(entity != null){
                        if(entity.liquids.currentAmount() < 0.01f){
                            // trigger achievement get event
                            PlayerData pd = playerDataHashMap.get(uuid);
                            if(pd != null){
                                if(!pd.achievements.containsKey(id)) pd.achievements.put(id, 0f);
                                float progress = pd.achievements.get(id);
                                if(progress < 100){

                                    // customize this for other achievements
                                    progress = 100;
                                    pd.achievements.put(id, progress);
                                    playerDataHashMap.put(uuid, pd);
                                    displayCompletion(event.player, event.tile.worldx(), event.tile.worldy());
                                }
                            }
                        }
                    }
                }
            }
        };
        all.add(chernobyl);

        kamikaze = new Achievement(3,"Kamikaze", "Withdraw blast compound"){
            @Override
            public void onItemWithdraw(EventType.WithdrawEvent event){
                if(event.player == null) return;
                String uuid = event.player.uuid;
                if(event.item == Items.blastCompound){
                    PlayerData pd = playerDataHashMap.get(uuid);
                    if(pd != null) {
                        if (!pd.achievements.containsKey(id)) pd.achievements.put(id, 0f);
                        float progress = pd.achievements.get(id);
                        if (progress < 100) {

                            progress = 100;
                            pd.achievements.put(id, progress);
                            playerDataHashMap.put(uuid, pd);
                            displayCompletion(event.player, event.tile.worldx(), event.tile.worldy());
                        }
                    }
                }
            }
        };
        all.add(kamikaze);

        heresy = new Achievement(4,"Heresy", "Build 2 routers next to each other"){
            @Override
            public void onBuild(EventType.BlockBuildEndEvent event){
                if(event.player == null) return;
                String uuid = event.player.uuid;
                if(event.tile.block() == Blocks.router && (event.tile.getNearby(1, 0).block()==Blocks.router || event.tile.getNearby(-1, 0).block()==Blocks.router) || event.tile.getNearby(0, 1).block()==Blocks.router || event.tile.getNearby(0, -1).block()==Blocks.router){
                    PlayerData pd = playerDataHashMap.get(uuid);
                    if(pd != null) {
                        if (!pd.achievements.containsKey(id)) pd.achievements.put(id, 0f);
                        float progress = pd.achievements.get(id);
                        if (progress < 100) {

                            progress = 100;
                            pd.achievements.put(id, progress);
                            playerDataHashMap.put(uuid, pd);
                            displayCompletion(event.player, event.tile.worldx(), event.tile.worldy());
                        }
                    }
                }
            }
        };
        all.add(heresy);

        ignition = new Achievement(5,"Ignition", "Build an impact reactor"){
            @Override
            public void onBuild(EventType.BlockBuildEndEvent event){
                if(event.player == null) return;
                String uuid = event.player.uuid;
                if(event.tile.block() == Blocks.impactReactor){
                    PlayerData pd = playerDataHashMap.get(uuid);
                    if(pd != null) {
                        if (!pd.achievements.containsKey(id)) pd.achievements.put(id, 0f);
                        float progress = pd.achievements.get(id);
                        if (progress < 100) {

                            progress = 100;
                            pd.achievements.put(id, progress);
                            playerDataHashMap.put(uuid, pd);
                            displayCompletion(event.player, event.tile.worldx(), event.tile.worldy());
                        }
                    }
                }
            }
        };
        all.add(ignition);

        nostalgia = new Achievement(6,"Nostalgia", "Play on the Boxfort map"){
            @Override
            public void onWave(){
                if(world.getMap().name().toLowerCase().equals("boxfort")) {
                    for (Player p : playerGroup.all()) {
                        PlayerData pd = playerDataHashMap.get(p.uuid);
                        if (pd != null) {
                            if (!pd.achievements.containsKey(id)) pd.achievements.put(id, 0f);
                            float progress = pd.achievements.get(id);
                            if (progress < 100) {

                                progress = 100;
                                displayCompletion(p);

                                pd.achievements.put(id, progress);
                                playerDataHashMap.put(p.uuid, pd);
                            }
                        }
                    }
                }
            }
        };
        all.add(nostalgia);

        shrooms = new Achievement(7,"Shrooms", "Build 100 cultivators"){
            @Override
            public void onBuild(EventType.BlockBuildEndEvent event){
                if(event.player == null) return;
                String uuid = event.player.uuid;
                if(event.tile.block() == Blocks.cultivator){
                    PlayerData pd = playerDataHashMap.get(uuid);
                    if(pd != null) {
                        if (!pd.achievements.containsKey(id)) pd.achievements.put(id, 0f);
                        float progress = pd.achievements.get(id);
                        if (progress < 100) {
                            progress++;
                            if(progress >= 100){
                                displayCompletion(event.player, event.tile.worldx(), event.tile.worldy());
                            }
                            pd.achievements.put(id, progress);
                            playerDataHashMap.put(uuid, pd);
                        }
                    }
                }
            }
        };
        all.add(shrooms);

        shrooooms = new Achievement(8,"SHROOOOMS", "Build 10000 cultivators"){
            @Override
            public void onBuild(EventType.BlockBuildEndEvent event){
                if(event.player == null) return;
                String uuid = event.player.uuid;
                if(event.tile.block() == Blocks.cultivator){
                    PlayerData pd = playerDataHashMap.get(uuid);
                    if(pd != null) {
                        if (!pd.achievements.containsKey(id)) pd.achievements.put(id, 0f);
                        float progress = pd.achievements.get(id);
                        if (progress < 100) {
                            progress = progress + 0.01f;
                            if(progress >= 100){
                                displayCompletion(event.player, event.tile.worldx(), event.tile.worldy());
                            }
                            pd.achievements.put(id, progress);
                            playerDataHashMap.put(uuid, pd);
                        }
                    }
                }
            }
        };
        all.add(shrooooms);

        snek = new Achievement(9,"The Snek", "Construct the snek"){
            @Override
            public void onBuild(EventType.BlockBuildEndEvent event){
                if(event.player == null) return;
                String uuid = event.player.uuid;
                if((event.tile.block() == Blocks.invertedSorter || event.tile.block() == Blocks.overflowGate) && (event.tile.getNearby(1, 0).block()==Blocks.invertedSorter || event.tile.getNearby(-1, 0).block()==Blocks.invertedSorter) || event.tile.getNearby(0, 1).block()==Blocks.invertedSorter || event.tile.getNearby(0, -1).block()==Blocks.invertedSorter){
                    PlayerData pd = playerDataHashMap.get(uuid);
                    if(pd != null) {
                        if (!pd.achievements.containsKey(id)) pd.achievements.put(id, 0f);
                        float progress = pd.achievements.get(id);
                        if (progress < 100) {

                            progress = 100;
                            pd.achievements.put(id, progress);
                            playerDataHashMap.put(uuid, pd);
                            displayCompletion(event.player, event.tile.worldx(), event.tile.worldy());
                        }
                    }
                }
            }
        };
        all.add(snek);

        mmo = new Achievement(10,"MMO", "Play a game with 50+ players online"){
            @Override
            public void onPlayerJoin(EventType.PlayerJoin event){
                int count = playerGroup.all().size;
                for(Player p : playerGroup.all()){
                    PlayerData pd = playerDataHashMap.get(p.uuid);
                    if(pd != null){
                        if(!pd.achievements.containsKey(id)) pd.achievements.put(id, 0f);
                        float progress = pd.achievements.get(id);
                        if(progress < 100){

                            progress = (count / 50f) * 100;
                            if(progress >= 100){
                                progress = 100;
                                displayCompletion(p);
                            }
                            pd.achievements.put(id, progress);
                            playerDataHashMap.put(p.uuid, pd);
                        }
                    }
                }
            }
        };
        all.add(mmo);

        fuzz = new Achievement(11,"What the fuzz", "Meet the fuzzbuck himself"){
            @Override
            public void onPlayerJoin(EventType.PlayerJoin event){
                if(event.player.isAdmin && event.player.name.contains("fuzz")) {
                    for (Player p : playerGroup.all()) {
                        PlayerData pd = playerDataHashMap.get(p.uuid);
                        if (pd != null) {
                            if (!pd.achievements.containsKey(id)) pd.achievements.put(id, 0f);
                            float progress = pd.achievements.get(id);
                            if (progress < 100) {

                                progress = 100;
                                displayCompletion(p);
                                pd.achievements.put(id, progress);
                                playerDataHashMap.put(p.uuid, pd);
                            }
                        }
                    }
                }
            }
        };
        all.add(fuzz);
    }

    public static class Achievement{
        public int id;
        public String name;
        public String desc;

        public void onWave(){}

        public void onPlayerJoin(EventType.PlayerJoin event){}

        public void onBuild(EventType.BlockBuildEndEvent event){}

        public void onItemDeposit(EventType.DepositEvent event){}

        public void onItemWithdraw(EventType.WithdrawEvent event){}

        public void onInterval(){}

        public void displayCompletion(Player player){
            Call.onInfoToast(player.con, "[yellow]Achievement get![]", 4f);
            Timer.schedule(() -> Call.onInfoToast(player.con, "[accent]" + name + "[] - " + desc, 14f), 4f);
        }

        public void displayCompletion(Player player, float worldx, float worldy){
            Call.onLabel(player.con,"[yellow]Achievement get![]", 14f, worldx, worldy);
            Call.onLabel(player.con,"[accent]" + name + "[] - " + desc, 14f, worldx - tilesize - 6, worldy - tilesize - 6);
            Call.onEffectReliable(player.con, Fx.shockwave, worldx, worldy, 0, Pal.accent);
        }

        public Achievement(int id, String name, String desc){
            this.id = id;
            this.name = name;
            this.desc = desc;
        }
    }
}
