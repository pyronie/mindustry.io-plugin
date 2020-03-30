package mindustry.plugin.datas;

import arc.struct.Array;
import arc.util.Log;
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

    public Achievements() {
        load();
    }

    public void load(){

        wave1000 = new Achievement(1,"5 hours later", "Reach wave 1000"){
            @Override
            public void onWave(){
                int wave = state.wave;
                for(Player p : playerGroup.all()){
                    PlayerData pd = playerDataHashMap.get(p.uuid);
                    if(pd != null){
                        if(!pd.achievements.containsKey(id)) pd.achievements.put(id, 0);
                        int progress = pd.achievements.get(id);
                        if(progress < 100){

                            progress = (wave / 1000) * 100;
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
                                if(!pd.achievements.containsKey(id)) pd.achievements.put(id, 0);
                                int progress = pd.achievements.get(id);
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
                        if (!pd.achievements.containsKey(id)) pd.achievements.put(id, 0);
                        int progress = pd.achievements.get(id);
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
        all.add(chernobyl);
    }

    public static class Achievement{
        public int id;
        public String name;
        public String desc;
        public boolean completed = false;

        public void onWave(){}

        public void onBuild(){}

        public void onItemDeposit(EventType.DepositEvent event){}

        public void onItemWithdraw(EventType.WithdrawEvent event){}

        public void displayCompletion(Player player){
            Call.onInfoToast(player.con, "[yellow]Achievement get![]", 4f);
            Timer.schedule(() -> Call.onInfoToast(player.con, "[accent]" + name + "[] - " + desc, 8f), 4f);
        }

        public void displayCompletion(Player player, float worldx, float worldy){
            Call.onLabel(player.con,"[yellow]Achievement get![]", 8f, worldx, worldy);
            Call.onLabel(player.con,"[accent]" + name + "[] - " + desc, 8f, worldx - tilesize - 6, worldy - tilesize - 6);
            Call.onEffectReliable(player.con, Fx.shockwave, worldx, worldy, 0, Pal.accent);
        }

        public Achievement(int id, String name, String desc){
            this.id = id;
            this.name = name;
            this.desc = desc;
        }
    }
}
