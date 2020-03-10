package mindustry.plugin;

public class PlayerData{
    public int rank;
    public int playTime = 0;
    public int buildingsBuilt = 0;
    public int gamesPlayed = 0;
    public boolean verified = false;
    public boolean banned = false;
    public long bannedUntil = 0;
    public String banReason = "";

    public String discordLink = "";

    public PlayerData(Integer rank) {
        this.rank = rank;
    }
}
