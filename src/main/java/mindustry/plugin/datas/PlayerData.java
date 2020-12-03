package mindustry.plugin.datas;

public class PlayerData{
    public String discord_id;
    public int highestWave;
    public boolean canInteract = true;
    public int rank;
    public int playTime = 0;
    public int buildingsBuilt = 0;
    public int gamesPlayed = 0;
    public boolean verified = false;
    public boolean banned = false;
    public long bannedUntil = 0;
    public String banReason = "";
    public String lastIP="";

    public PlayerData() { }
}
