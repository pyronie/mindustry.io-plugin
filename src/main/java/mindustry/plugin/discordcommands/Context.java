package mindustry.plugin.discordcommands;

import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.MessageAuthor;
import org.javacord.api.event.message.MessageCreateEvent;

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
}
