package mindustry.plugin.datas;

import mindustry.entities.type.Player;

import static mindustry.plugin.datas.Achievements.*;

public class PlayerData implements Cloneable{
    public int highestWave;
    public Achievements achievements;
    public int role;

    public long bannedUntil = 0;
    public String banReason = "";

    public PlayerData(Player player) {
        achievements = new Achievements(player.uuid);
    }

    public void reprocess(Player player){
        if(banReason == null || achievements == null){
            this.banReason = "";
            this.achievements = new Achievements(player.uuid);
        }
    }

    public Object clone()throws CloneNotSupportedException{return super.clone();}
}
