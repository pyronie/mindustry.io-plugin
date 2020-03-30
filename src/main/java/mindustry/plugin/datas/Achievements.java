package mindustry.plugin.datas;

import arc.struct.Array;
import arc.util.Timer;
import mindustry.Vars;
import mindustry.entities.type.Player;
import mindustry.gen.Call;

import java.util.concurrent.CompletableFuture;

import static mindustry.Vars.*;

public class Achievements {
    public static String owner;
    public static Array<Achievement> all;
    public static Achievement current;

    public static Achievement wave1000;
    public static Achievement chernobyl;
    public static Achievement walle;

    public Achievements(String uuid) {
        owner = uuid;
        load();
    }

    public void setNext(){
        for(Achievement achievement : all){
            if(!achievement.completed){
                current = achievement;
            }
        }
    };

    public void load(){
        wave1000 = new Achievement("5 hours later", "Reach wave 1000"){
            @Override
            public void onWave(){
                int wave = state.wave;
                if(wave >= 1000){
                    complete(owner);
                }
            }
        };
        all.add(wave1000);

        chernobyl = new Achievement("Chernobyl", "Put thorium in an empty, unsupplied reactor"){
            @Override
            public void onItemDeposit(){

            }
        };
        all.add(chernobyl);
    }

    public static class Achievement{
        boolean completed = false;
        String name;
        String desc;

        public void onWave(){}

        public void onBuild(){}

        public void onItemDeposit(){}

        public void complete(String uuid){
            Player player = playerGroup.all().find(p -> p.uuid.equals(uuid));
            if(player != null){
                this.completed = true;
                displayCompletion(player);
            }
        }

        public void displayCompletion(Player player){
            Call.onInfoToast(player.con, "[yellow]Achievement get![]", 5f);
            Timer.schedule(() -> Call.onInfoToast(player.con, "[accent]" + name + "[] " + desc, 15f), 5f);
        }

        public Achievement(String name, String desc){
            this.name = name;
            this.desc = desc;
        }
    }
}
