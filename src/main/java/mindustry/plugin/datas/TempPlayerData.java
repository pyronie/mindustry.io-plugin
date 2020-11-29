package mindustry.plugin.datas;

import mindustry.entities.bullet.BulletType;
import mindustry.world.Tile;

public class TempPlayerData {
    public BulletType bt;
    public float sclVelocity;
    public float sclLifetime;
    public float sclDamage;
    public String tag;
    public Tile tapTile;
    public boolean inspector;
    public boolean frozen = false;

    public TempPlayerData(){}
}
