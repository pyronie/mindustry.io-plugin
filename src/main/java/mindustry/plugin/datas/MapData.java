package mindustry.plugin.datas;

public class MapData implements Cloneable{
    public String name = "";
    public String description = "";
    public String author = "";
    public int timesPlayed = 0;
    public int maxWave = 0;

    public MapData(String name, String description, String author) {
        this.name = name;
        this.description = description;
        this.author = author;
    }

    public void reprocess(){
    }

}
