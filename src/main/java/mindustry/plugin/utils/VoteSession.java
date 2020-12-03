package mindustry.plugin.utils;

import arc.struct.ObjectSet;
import arc.util.Strings;
import mindustry.gen.Call;
import mindustry.gen.Groups;
import mindustry.gen.Player;
import mindustry.maps.Map;

import static mindustry.Vars.netServer;

public class VoteSession{
    Map target;
    public ObjectSet<String> voted = new ObjectSet<>();
    VoteSession[] map;
    int votes;

    public VoteSession(VoteSession[] map, Map target){
        this.target = target;
        this.map = map;
    }

    public int votesRequired(){
        return (int) (Groups.player.size() / 1.5f);
    }

    public void vote(Player player, int d){
        votes += d;
        voted.addAll(player.uuid(), netServer.admins.getInfo(player.uuid()).lastIP);

        Call.sendMessage(Strings.format("[orange]@[lightgray] has voted to change the map to[orange] @[].[accent] (@/@)\n[lightgray]Type[orange] /rtv to agree.",
                player.name, target.name(), votes, votesRequired()));

        checkPass();
    }

    void checkPass(){
        if(votes >= votesRequired()){
            Call.sendMessage(Strings.format("[orange]Vote passed.[scarlet] changing map to @.", target.name()));
            Funcs.changeMap(target);
            map[0] = null;
        }
    }
}
