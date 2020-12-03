package mindustry.plugin.utils;

import mindustry.gen.Call;

import static mindustry.Vars.state;
import static mindustry.plugin.ioMain.*;

public class MapRules {

    public static void run(){
        Call.sendChatMessage(state.map.description());
        minutesPassed = 0;
    }
}
