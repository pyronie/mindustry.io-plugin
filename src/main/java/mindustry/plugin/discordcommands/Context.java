package mindustry.plugin.discordcommands;

import mindustry.plugin.Utils;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.MessageAuthor;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;

import java.util.HashMap;

/** Represents the context in which a command was called */
public class Context {
    /** Source event */
    public MessageCreateEvent event;
    public TextChannel channel;
    public MessageAuthor author;

    public Context(MessageCreateEvent event) {
        this.event = event;
        this.channel = event.getChannel();
        this.author = event.getMessageAuthor();
    }

    public void sendEmbed(String title){
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle(title);
        channel.sendMessage(eb);
    }

    public void sendEmbed(boolean success, String title){
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle(title);
        if(success){
            eb.setColor(Utils.Pals.success);
        } else{
            eb.setColor(Utils.Pals.error);
        }
        channel.sendMessage(eb);
    }

    public void sendEmbed(String title, String description){
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle(title);
        eb.setDescription(description);
        channel.sendMessage(eb);
    }

    public void sendEmbed(boolean success, String title, String description){
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle(title);
        eb.setDescription(description);
        if(success){
            eb.setColor(Utils.Pals.success);
        } else{
            eb.setColor(Utils.Pals.error);
        }
        channel.sendMessage(eb);
    }

    public void sendEmbed(boolean success, String title, HashMap<String, String> fields){
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle(title);
        if(success){
            eb.setColor(Utils.Pals.success);
        } else{
            eb.setColor(Utils.Pals.error);
        }
        for(String name : fields.keySet()){
            String desc = fields.get(name);
            eb.addField(name, desc);
        }
        channel.sendMessage(eb);
    }

    public void sendEmbed(String title, String description, HashMap<String, String> fields){
        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle(title);
        eb.setDescription(description);
        for(String name : fields.keySet()){
            String desc = fields.get(name);
            eb.addField(name, desc);
        }
        channel.sendMessage(eb);
    }
}
