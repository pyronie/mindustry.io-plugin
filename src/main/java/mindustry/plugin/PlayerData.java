package mindustry.plugin;

import arc.struct.Array;

import java.io.Serializable;

public class PlayerData implements Serializable {
    public int rank;
    public int playTime = 0;
    public int buildingsBuilt = 0;
    public int gamesPlayed = 0;
    public boolean verified = true;

    public Array<String> ips = new Array<>();

    public PlayerData(Integer rank) {
        this.rank = rank;
    }

}
