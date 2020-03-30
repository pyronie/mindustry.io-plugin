package mindustry.plugin.datas;

import java.util.HashMap;

public class PlayerData{
    public int highestWave;
    public int role;

    public long bannedUntil = 0;
    public String banReason = "";

    public HashMap<Integer, Integer> achievements; // achievement id, progress <0-100>

    public PlayerData() {
        achievements = new HashMap<>();
    }
}
