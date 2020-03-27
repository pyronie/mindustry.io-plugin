package mindustry.plugin;

import arc.files.Fi;
import arc.math.Mathf;
import arc.util.CommandHandler;
import arc.util.Log;
import arc.util.Timer;
import mindustry.maps.Map;

import mindustry.entities.type.Player;
import mindustry.game.Team;
import mindustry.game.Teams.TeamData;
import mindustry.gen.Call;
import mindustry.mod.ContentParser;
import mindustry.plugin.datas.PlayerData;
import mindustry.plugin.discordcommands.RoleRestrictedCommand;
import mindustry.world.blocks.storage.CoreBlock.CoreEntity;
import mindustry.world.modules.ItemModule;
import mindustry.plugin.discordcommands.Command;
import mindustry.plugin.discordcommands.Context;
import mindustry.plugin.discordcommands.DiscordCommands;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageAuthor;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.permission.Role;
import redis.clients.jedis.exceptions.JedisConnectionException;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static mindustry.Vars.*;
import static mindustry.plugin.Utils.*;
import static mindustry.plugin.ioMain.*;
import static mindustry.plugin.ioMain.getTextChannel;

public class ComCommands {
    public void registerCommands(CommandHandler handler) {
        handler.<Context>register("chat", "<message...>", "Send a message to in-game chat.", (args, ctx) -> {
            if(args[0].length() < chatMessageMaxSize){
                Call.sendMessage("[sky]" + ctx.author.getDiscriminatedName() + " @discord >[] " + args[0]);
                ctx.sendEmbed(true, ":mailbox_with_mail: **message sent!**", "``" + escapeCharacters(args[0]) + "``");
            } else{
                ctx.sendEmbed(false, ":exclamation: **message too big!**", "maximum size: **" + chatMessageMaxSize + " characters**");
            }
        });

        handler.<Context>register("map","<map...>", "Preview/download a map from the playlist.", (args, ctx) -> {
            Map map = getMapBySelector(args[0].trim());
            if (map != null){
                try {
                    ContentHandler.Map visualMap = contentHandler.parseMap(map.file.read());
                    Fi mapFile = map.file;
                    File imageFile = new File("iocontent/image_" + mapFile.name().replaceAll(".msav", ".png"));
                    ImageIO.write(visualMap.image, "png", imageFile);

                    EmbedBuilder eb = new EmbedBuilder().setColor(Pals.success).setTitle(":map: **" + escapeCharacters(map.name()) + "**").setDescription(escapeCharacters(map.description())).setAuthor(escapeCharacters(map.author()));
                    eb.setImage(imageFile);
                    ctx.channel.sendMessage(eb, mapFile.file());
                } catch (IOException e) {
                    ctx.sendEmbed(false, ":eyes: **internal server error**");
                    e.printStackTrace();
                }
            }else{
                ctx.sendEmbed(false, ":mag: map **" + escapeCharacters(args[0]) + "** not found");
            }
        });

        handler.<Context>register("players","Get all online in-game players.", (args, ctx) -> {
            //todo: fix this

            EmbedBuilder eb = new EmbedBuilder().setColor(Pals.success).setTitle(":satellite: **players online: **" + playerGroup.all().size);
            for (int rank : rankNames.keySet()) {
                String rankName = rankNames.get(rank).name;
                StringBuilder players = new StringBuilder();
                playerGroup.forEach(player -> {
                    try {
                        PlayerData pd = getData(player.uuid);
                        if (pd != null && pd.rank == rank) {
                            players.append(escapeCharacters(player.name)).append("\n");
                        }
                    } catch(JedisConnectionException ignored){}
                });
                eb.addField(rankName, players.toString());
            }
            ctx.channel.sendMessage(eb);
        });

        handler.<Context>register("status", "View the status of this server.", (args, ctx) -> {
            HashMap<String, String> fields = new HashMap<>();
            fields.put("players", String.valueOf(playerGroup.all().size));
            fields.put("map", escapeCharacters(world.getMap().name()));
            fields.put("wave", String.valueOf(state.wave));

            ctx.sendEmbed(true, ":desktop: **" + serverName + "**", fields);
        });

        /*
        handler.registerCommand(new Command("help") {
            {
                help = "Display all available commands and their usage.";
            }
            public void run(Context ctx) {
                EmbedBuilder embed = new EmbedBuilder()
                        .setTitle("Public commands:");
                EmbedBuilder embed2 = new EmbedBuilder()
                        .setTitle("Restricted commands:");
                for(Command command : handler.getAllCommands()) {
                    if(command instanceof RoleRestrictedCommand) {
                        embed2.addField("**" + command.name + "**", command.help);
                    } else {
                        embed.addField("**" + command.name + "**", command.help);
                    }
                }
                ctx.channel.sendMessage(embed2);
                ctx.channel.sendMessage(embed);
            }
        });

        handler.registerCommand(new Command("redeem"){
            {
                help = "<name|id> Promote your in-game rank. [NOTE: Abusing this power and giving it to other players will result in a ban.]";
            }

            public void run(Context ctx) {
                CompletableFuture.runAsync(() -> {
                    EmbedBuilder eb = new EmbedBuilder();
                    String target = "";
                    if (ctx.args.length > 1) {
                        target = ctx.args[1];
                    }
                    List<Role> authorRoles = ctx.author.asUser().get().getRoles(ctx.event.getServer().get()); // javacord gay
                    List<String> roles = new ArrayList<>();
                    for (Role r : authorRoles) {
                        if (r != null) {
                            roles.add(r.getIdAsString());
                        }
                    }
                    if (target.length() > 0) {
                        int rank = 0;
                        for (String role : roles) {
                            if (rankRoles.containsKey(role)) {
                                if (rankRoles.get(role) > rank) {
                                    rank = rankRoles.get(role);
                                }
                            }
                        }
                        Player player = findPlayer(target);
                        if (player != null && rank > 0) {
                            PlayerData pd = getData(player.uuid);
                            if (pd != null) {
                                pd.rank = rank;
                                setData(player.uuid, pd);
                            }
                            eb.setTitle("Command executed successfully");
                            eb.setDescription("Promoted " + escapeCharacters(player.name) + " to " + escapeColorCodes(rankNames.get(rank).name) + ".");
                            ctx.channel.sendMessage(eb);
                            player.con.kick("Your rank was modified, please rejoin.", 0);
                        } else {
                            eb.setTitle("Command terminated");
                            eb.setDescription("Player not online or not found.");
                            ctx.channel.sendMessage(eb);
                        }
                    } else {
                        eb.setTitle("Command terminated");
                        eb.setDescription("Invalid arguments provided or no roles to redeem.");
                        ctx.channel.sendMessage(eb);
                    }
                });
            }

        });

        TextChannel warningsChannel = null;
        if (ioMain.data.has("warnings_chat_channel_id")) {
            warningsChannel = getTextChannel(ioMain.data.getString("warnings_chat_channel_id"));
        }

        TextChannel finalWarningsChannel = warningsChannel;
        handler.registerCommand(new Command("link") {
            {
                help = "<player/id> Link your discord account with your in-game account (receive special benefits)";
            }
            public void run(Context ctx) {
                CompletableFuture.runAsync(() -> {
                    if (ctx.event.isPrivateMessage()) return;
                    EmbedBuilder eb = new EmbedBuilder();
                    ctx.message = escapeCharacters(ctx.message);
                    Player p = findPlayer(ctx.message);
                    if (p != null) {
                        String uuid = p.uuid;
                        PlayerData pd = getData(uuid);

                        if (pd != null) {
                            if (pd.discordLink.length() > 0) {
                                eb.setTitle("That player already has an active discord link!");
                                eb.setDescription("If that's you, and you wish to change your discord link, use the /removelink command in-game.");
                                eb.setColor(Pals.warning);
                                ctx.channel.sendMessage(eb);
                            } else {
                                p.passPhrase = String.valueOf(Mathf.random(1000, 9999));
                                eb.setTitle("<a:loading:686693525907177519> Attempting link with " + escapeCharacters(p.name));
                                eb.addField("Link PIN", p.passPhrase);
                                eb.setDescription("Now, use the /link " + p.passPhrase + "** command in game.");
                                CompletableFuture<Message> msg = ctx.channel.sendMessage(eb);
                                setData(uuid, pd);
                                Timer.schedule(() -> {
                                    PlayerData pd2 = getData(uuid);
                                    if (pd2 != null) {
                                        EmbedBuilder eb2 = new EmbedBuilder();
                                        if (p.passPhrase.equals("OK")) {
                                            pd2.discordLink = ctx.author.getIdAsString();
                                            pd2.verified = true;
                                            setData(p.uuid, pd2);
                                            eb2.setTitle(":white_check_mark: Link with " + escapeCharacters(p.name) + " successful!");
                                            eb2.setDescription("Thank you for linking your account.");
                                            eb2.setColor(Pals.success);
                                            msg.thenAccept(m -> {
                                                m.edit(eb2);
                                            });
                                            if (finalWarningsChannel != null) {
                                                EmbedBuilder eb3 = new EmbedBuilder();
                                                eb3.setTitle(ctx.author.getDiscriminatedName() + " <-> " + escapeCharacters(p.name) + " link successful.");
                                                eb3.addField("UUID", p.uuid);
                                                finalWarningsChannel.sendMessage(eb3);
                                            }
                                        } else {
                                            eb2.setTitle(":x: Link with " + escapeCharacters(p.name) + " failed.");
                                            eb2.setDescription("Timed out. You have 15 seconds to use the /link command in game.");
                                            eb2.setColor(Pals.error);
                                            msg.thenAccept(m -> {
                                                m.edit(eb2);
                                            });
                                        }
                                        setData(uuid, pd2);
                                    }
                                    p.passPhrase = "";
                                }, 15);
                                p.sendMessage("[#7289da]\uE848[#99aab5] A discord link was prompted to your account by " + ctx.author.getDiscriminatedName());
                                p.sendMessage("[#7289da]\uE848[#99aab5] If this is you, use the /link command with the provided PIN on discord.");
                            }
                        }
                    } else {
                        eb.setTitle("Command terminated");
                        eb.setDescription("Player " + ctx.message + " not found in " + ioMain.serverName + ".\nAre you using the correct prefix?");
                        eb.setColor(Pals.error);
                        ctx.channel.sendMessage(eb);
                    }
                });
            }
        });*/
    }
}
