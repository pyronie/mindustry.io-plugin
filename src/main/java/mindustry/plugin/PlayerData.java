package mindustry.plugin;

public class PlayerData{
    public int rank;
    public int playTime = 0;
    public int buildingsBuilt = 0;
    public int gamesPlayed = 0;
    public boolean verified = false;
    public boolean banned = false;

    public PlayerData(Integer rank) {
        this.rank = rank;
    }
}
