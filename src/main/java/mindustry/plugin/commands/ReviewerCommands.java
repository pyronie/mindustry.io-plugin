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

    }
    */
}