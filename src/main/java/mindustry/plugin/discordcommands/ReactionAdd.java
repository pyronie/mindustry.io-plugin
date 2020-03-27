package mindustry.plugin.discordcommands;

import arc.Core;
import arc.files.Fi;
import arc.util.Log;
import mindustry.Vars;
import mindustry.graphics.Pal;
import mindustry.maps.Map;
import mindustry.plugin.Utils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.RestAction;

import java.io.File;
import java.nio.file.Files;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static mindustry.Vars.*;
import static mindustry.plugin.ioMain.*;
import static net.dv8tion.jda.api.entities.Message.*;

/** Represents a registry of commands */
public class ReactionAdd extends ListenerAdapter {
    public ReactionAdd() {
    }

    @Override
    public void onMessageReactionAdd(MessageReactionAddEvent event) {
        MessageReaction reaction = event.getReaction();
        Message message = event.getTextChannel().retrieveMessageById(event.getMessageId()).complete();
        MessageEmbed embed = (message.getEmbeds().size() > 0 ? message.getEmbeds().get(0) : null);
        Emote emote = reaction.getReactionEmote().getEmote();
        Member member = event.getMember();
        
        if(event.getUser() == api.getSelfUser() || event.getUser() == null) return;
        if(embed != null && member != null){
            if(message.getChannel() == mapSubmissions){ // if reacted to a message in map submissions
                if(member.getRoles().contains(mapreviewer)){
                    Log.info(emote.getId());
                    if(emote.getId().equals("693162979616751616")){
                        // approved, upload map
                        Attachment attachment = message.getAttachments().get(0);
                        if(attachment == null) return;

                        attachment.downloadToFile(new File(Core.settings.getDataDirectory().child("maps").child(attachment.getFileName()).path())).thenAccept(file -> {
                            maps.reload();
                            Map map = maps.all().find(m -> m.file.name().equals(file.getName()));
                            User userByTag = api.getUserByTag(embed.getAuthor().getName());
                            if(userByTag != null){ userByTag.openPrivateChannel().queue(pm -> { pm.sendMessage(new EmbedBuilder().setTitle(":clap: **your map was approved!**").setColor(Utils.Pals.success).setDescription(Utils.escapeCharacters(map.name()) + " was approved into " + serverName + "'s playlist by " + member.getUser().getAsTag()).build()).queue(); });}
                            message.getChannel().sendMessage(new EmbedBuilder().setTitle(":thumbsup: **" + Utils.escapeCharacters(map.name()) + "** approved and added into " + serverName + "'s playlist").setColor(Utils.Pals.success).build()).queue(message1 -> {
                                message.delete().queue();
                                message1.delete().queueAfter(15, TimeUnit.SECONDS);
                            });
                        });
                    }else if(emote.getId().equals("693162961761730723")){
                        Log.info("disapproving");
                        User userByTag = api.getUserByTag(embed.getAuthor().getName());
                        if(userByTag != null){ userByTag.openPrivateChannel().queue(pm -> { pm.sendMessage(new EmbedBuilder().setTitle(":anguished: **your map was denied..**").setColor(Utils.Pals.error).setDescription("disapproved by " + member.getUser().getAsTag()).build()).queue(); });}
                        message.getChannel().sendMessage(new EmbedBuilder().setTitle(":thumbsdown: denied. ").setColor(Utils.Pals.error).build()).queue(message1 -> {
                            message.delete().queue();
                            message1.delete().queueAfter(15, TimeUnit.SECONDS);
                        });
                    }
                }else{
                    reaction.removeReaction(event.getUser()).complete();
                }
            }
        }
    }
}