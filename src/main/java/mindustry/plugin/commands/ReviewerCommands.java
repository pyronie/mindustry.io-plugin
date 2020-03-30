package mindustry.plugin.commands;

import arc.Events;
import arc.util.CommandHandler;
import mindustry.game.EventType;
import mindustry.game.Team;
import mindustry.maps.Map;
import mindustry.plugin.discord.Context;
import mindustry.plugin.discord.Loader;
import mindustry.plugin.utils.Funcs;

import static mindustry.Vars.*;
import static mindustry.plugin.discord.Loader.*;
import static mindustry.plugin.utils.Funcs.*;

public class ReviewerCommands {
    public ReviewerCommands(){

    }

    public void registerCommands(CommandHandler handler){
        handler.<Context>register("removemap", "<map...>", "Remove the specified map from the playlist.", (args, ctx) -> {
            Map map = getMapBySelector(args[0]);
            if(map != null){
                maps.removeMap(map);
                maps.reload();
                ctx.sendEmbed(true, ":knife: **" + map.name() + "** was murdered successfully");
            } else{
                ctx.sendEmbed(false, ":knife: *" + Funcs.escapeCharacters(args[0]) + "* not found", "display all maps with **" + prefix + "maps**");
            }
        });

        handler.<Context>register("changemap", "[map...]", "Change the map to the provided one, or to a random one if no map is provided. Applies to " + serverName, (args, ctx) -> {
            if(args.length > 0){
                Map map = getMapBySelector(args[0]);
                if(map != null){
                    changeMap(map);
                    ctx.sendEmbed(true, ":mountain_snow: map changed to *"  + escapeCharacters(map.name()) + "*");
                }else{
                    ctx.sendEmbed(false, ":mountain_snow: *" + Funcs.escapeCharacters(args[0]) + "* not found", "display all maps with **" + prefix + "maps**");
                }
            }else{
                Events.fire(new EventType.GameOverEvent(Team.sharded));
                ctx.sendEmbed(true, ":mountain_snow: gameover event trigerred");
            }
        });
    }
    /**

    public void registerCommands(DiscordCommands handler) {

            handler.registerCommand(new RoleRestrictedCommand("killunits") {
                {
                    help = "<playerid|ip|name> <unit> Kills all units of the team of the specified player";
                    role = banRole;
                }
                public void run(Context ctx) {
                    EmbedBuilder eb = new EmbedBuilder();
                    String target = ctx.args[1];
                    String targetUnit = ctx.args[2];
                    UnitType desiredUnit = UnitTypes.dagger;
                    if(target.length() > 0 && targetUnit.length() > 0) {
                        try {
                            Field field = UnitTypes.class.getDeclaredField(targetUnit);
                            desiredUnit = (UnitType)field.get(null);
                        } catch (NoSuchFieldException | IllegalAccessException ignored) {}

                        Player player = findPlayer(target);
                        if(player!=null){
                            int amount = 0;
                            for(BaseUnit unit : Vars.unitGroup.all()) {
                                if(unit.getTeam() == player.getTeam()){
                                    if(unit.getType() == desiredUnit) {
                                        unit.kill();
                                        amount++;
                                    }
                                }
                            }
                            eb.setTitle("Command executed successfully.");
                            eb.setDescription("Killed " + amount + " " + targetUnit + "s on team " + player.getTeam());
                            ctx.channel.sendMessage(eb);
                        }
                    } else{
                        eb.setTitle("Command terminated");
                        eb.setDescription("Invalid arguments provided.");
                        eb.setColor(Pals.error);
                        ctx.channel.sendMessage(eb);
                    }
                }
            });

            handler.registerCommand(new RoleRestrictedCommand("setblock") {
                {
                    help = "<playerid|ip|name> <block> Create a block at the player's current location and on the player's current team.";
                    role = banRole;
                }
                public void run(Context ctx) {
                    String target = ctx.args[1];
                    String targetBlock = ctx.args[2];
                    Block desiredBlock = Blocks.copperWall;

                    try {
                        Field field = Blocks.class.getDeclaredField(targetBlock);
                        desiredBlock = (Block)field.get(null);
                    } catch (NoSuchFieldException | IllegalAccessException ignored) {}

                    EmbedBuilder eb = new EmbedBuilder();
                    Player player = findPlayer(target);

                    if(player!=null){
                        float x = player.getX();
                        float y = player.getY();
                        Tile tile = world.tileWorld(x, y);
                        tile.setNet(desiredBlock, player.getTeam(), 0);

                        eb.setTitle("Command executed successfully.");
                        eb.setDescription("Spawned " + desiredBlock.name + " on " + Utils.escapeCharacters(player.name) + "'s position.");
                        ctx.channel.sendMessage(eb);
                    } else{
                        eb.setTitle("Command terminated");
                        eb.setDescription("Invalid arguments provided.");
                        eb.setColor(Pals.error);
                        ctx.channel.sendMessage(eb);
                    }
                }
            });

            handler.registerCommand(new RoleRestrictedCommand("weaponmod") { // OH NO
                {
                    help = "<playerid|ip|name|all(oh no)> <bullet-type> <lifetime-modifier> <velocity-modifier> Mod the current weapon of a player.";
                    role = banRole;
                }
                public void run(Context ctx) {
                    EmbedBuilder eb = new EmbedBuilder();

                    String target = ctx.args[1];
                    String targetBullet = ctx.args[2];
                    float targetL = Float.parseFloat(ctx.args[3]);
                    float targetV = Float.parseFloat(ctx.args[4]);
                    BulletType desiredBullet = null;

                    if(target.length() > 0 && targetBullet.length() > 0) {
                        try {
                            Field field = Bullets.class.getDeclaredField(targetBullet);
                            desiredBullet = (BulletType)field.get(null);
                        } catch (NoSuchFieldException | IllegalAccessException ignored) {}

                        if(target.equals("all")){
                            eb.setTitle("Command executed");
                            for(Player p : playerGroup.all()){
                                if (desiredBullet == null) {
                                    p.bt = null;
                                    eb.setDescription("Reverted everyone's weapon to default.");
                                } else {
                                    p.bt = desiredBullet;
                                    p.sclLifetime = targetL;
                                    p.sclVelocity = targetV;
                                    eb.setDescription("Modded everyone's weapon to " + targetBullet + " with " + targetL + "x lifetime modifier and " + targetV + "x velocity modifier.");
                                }
                            }
                            ctx.channel.sendMessage(eb);
                        }

                        Player player = findPlayer(target);
                        if(player!=null){
                            eb.setTitle("Command executed");
                            if(desiredBullet == null){
                                player.bt = null;
                                eb.setDescription("Reverted " + escapeCharacters(player.name) + "'s weapon to default.");
                            } else{
                                player.bt = desiredBullet;
                                player.sclLifetime = targetL;
                                player.sclVelocity = targetV;

                                eb.setDescription("Modded " + escapeCharacters(player.name) + "'s weapon to " + targetBullet + " with " + targetL + "x lifetime modifier and " + targetV + "x velocity modifier.");
                            }
                            ctx.channel.sendMessage(eb);
                        }
                    } else{
                        eb.setTitle("Command terminated");
                        eb.setDescription("Invalid arguments provided.");
                        eb.setColor(Pals.error);
                        ctx.channel.sendMessage(eb);
                    }
                }
            });

        }

            handler.registerCommand(new Command("sendm"){ // use sendm to send embed messages when needed locally, disable for now
                public void run(Context ctx){
                    EmbedBuilder eb = new EmbedBuilder()
                            .setColor(Utils.Pals.info)
                            .setTitle("Support mindustry.io by donating, and receive custom ranks!")
                            .setUrl("https://donate.mindustry.io/")
                            .setDescription("By donating, you directly help me pay for the monthly server bills I receive for hosting 4 servers with **150+** concurrent players daily.")
                            .addField("VIP", "**VIP** is obtainable through __nitro boosting__ the server or __donating $1.59+__ to the server.", false)
                            .addField("__**MVP**__", "**MVP** is a more enchanced **vip** rank, obtainable only through __donating $3.39+__ to the server.", false)
                            .addField("Where do I get it?", "You can purchase **vip** & **mvp** ranks here: https://donate.mindustry.io", false)
                            .addField("\uD83E\uDD14 io is pay2win???", "Nope. All perks vips & mvp's gain are aesthetic items **or** items that indirectly help the team. Powerful commands that could give you an advantage are __disabled on pvp.__", true);
                    ctx.channel.sendMessage(eb);
                }
            });


        if(data.has("mapSubmissions_roleid")){
            String reviewerRole = data.getString("mapSubmissions_roleid");

            handler.registerCommand(new RoleRestrictedCommand("removemap") {
                {
                    help = "<mapname/mapid> Remove a map from the playlist (use mapname/mapid retrieved from the %maps command)".replace("%", ioMain.prefix);
                    role = reviewerRole;
                }
                @Override
                public void run(Context ctx) {
                    EmbedBuilder eb = new EmbedBuilder();
                    if (ctx.args.length < 2) {
                        eb.setTitle("Command terminated.");
                        eb.setColor(Pals.error);
                        eb.setDescription("Not enough arguments, use `%removemap <mapname/mapid>`".replace("%", ioMain.prefix));
                        ctx.channel.sendMessage(eb);
                        return;
                    }
                    Map found = getMapBySelector(ctx.message.trim());
                    if (found == null) {
                        eb.setTitle("Command terminated.");
                        eb.setColor(Pals.error);
                        eb.setDescription("Map not found");
                        ctx.channel.sendMessage(eb);
                        return;
                    }

                    maps.removeMap(found);
                    maps.reload();

                    eb.setTitle("Command executed.");
                    eb.setDescription(found.name() + " was successfully removed from the playlist.");
                    ctx.channel.sendMessage(eb);
                }
            });
        }
    }
    */
}