package mindustry.plugin.datas;

public class PlayerData implements Cloneable{
    public int highiestWave;
    public int achievements;
    public int role;

    public long bannedUntil = 0;
    public String banReason = "";

    public PlayerData() {}

    public void reprocess(){
        if(banReason == null) this.banReason = "";
    }

    public Object clone()throws CloneNotSupportedException{return super.clone();}
}
