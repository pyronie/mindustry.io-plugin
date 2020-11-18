package mindustry.plugin.datas;

import mindustry.entities.bullet.BulletType;
import mindustry.world.Tile;

import java.util.HashMap;

public class PlayerData{
    public int highestWave;
    public int role;
    public String tag;

    public Tile tapTile;
    public boolean inspector;
    public boolean canInteract = false;

    public BulletType bt;
    public float sclVelocity;
    public float sclLifetime;
    public float sclDamage;

    public long bannedUntil = 0;
    public String banReason = "";

    public HashMap<Integer, Float> achievements; // achievement id, progress <0-100>

    public PlayerData() {
        achievements = new HashMap<>();
    }
}
