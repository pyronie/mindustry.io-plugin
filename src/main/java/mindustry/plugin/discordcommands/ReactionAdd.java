package mindustry.plugin.discordcommands;

import arc.util.Log;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.RestAction;

import java.util.Objects;

import static mindustry.plugin.ioMain.*;

/** Represents a registry of commands */
public class ReactionAdd extends ListenerAdapter {
    public ReactionAdd() {
    }

    @Override
    public void onMessageReactionAdd(MessageReactionAddEvent event) {
        MessageReaction reaction = event.getReaction();
        Message message = event.getTextChannel().retrieveMessageById(event.getMessageId()).complete();
        MessageEmbed embed = message.getEmbeds().get(0);
        Emote emote = reaction.getReactionEmote().getEmote();
        Member member = event.getMember();


        if(Objects.requireNonNull(member).getUser() == api.getSelfUser()) return;

        if(embed != null){
            if(message.getChannel() == mapSubmissions){ // if reacted to a message in map submissions
                if(member.getRoles().contains(mapreviewer)){
                    if(emote.getId().equals("693182504810577991")){
                        Log.info("approving");
                    }else if(emote.getId().equals("693182516840103946")){
                        Log.info("disapproving");
                    }
                }else{
                    reaction.removeReaction().complete();
                }
            }
        }
    }
}