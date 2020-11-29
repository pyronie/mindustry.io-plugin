package mindustry.plugin.datas;

public class PlayerData{
    public int highestWave;
    public boolean canInteract = true;

    public String uuid;
    public int rank;
    public int playTime = 0;
    public int buildingsBuilt = 0;
    public int gamesPlayed = 0;
    public boolean verified = false;
    public boolean banned = false;
    public long bannedUntil = 0;
    public String banReason = "";

    public PlayerData(int rank) { this.rank = rank; }
}
