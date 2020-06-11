package mindustry.plugin.discord;

import arc.files.Fi;
import arc.util.CommandHandler;
import arc.util.Log;
import mindustry.io.SaveIO;
import mindustry.plugin.utils.ContentHandler;
import mindustry.plugin.utils.Funcs;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import javax.imageio.ImageIO;
import java.io.*;
import java.util.List;
import java.util.zip.InflaterInputStream;

import static mindustry.plugin.discord.Loader.*;
import static mindustry.plugin.utils.Funcs.assets;
import static mindustry.plugin.utils.Funcs.escapeCharacters;

/** Represents a registry of commands */
public class DiscordCommands extends ListenerAdapter {
    private boolean stop = false;
    public DiscordCommands() {
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        // disable DMs & self messages
        if(event.isFromType(ChannelType.PRIVATE)) return;
        if(event.getMessage().getAuthor() == api.getSelfUser()) return;

        Member member = event.getMember();
        if(member == null) return;

        Context ctx = new Context(event);
        String content = event.getMessage().getContentRaw();

        List<Role> roles = member.getRoles();
        stop = false;
        if(!handleCommand(bt.publicHandler, content, ctx)){
            if(roles.contains(mapreviewer)){
                if(handleCommand(bt.reviewerHandler, content, ctx)) stop = true;
            }
            if(roles.contains(moderator) && !stop){
                if(handleCommand(bt.moderatorHandler, content, ctx)) stop = true;
            }
            if (!stop) ctx.sendEmbed(false, ":interrobang: no permission or command doesn't exist", "consider using the **" + prefix + "help** command");
        }
    }

    public boolean handleCommand(CommandHandler handler, String contentRaw, Context ctx){
        boolean returnant = true;
        CommandHandler.CommandResponse response = handler.handleMessage(contentRaw, ctx);

        if (response.type != CommandHandler.ResponseType.noCommand) {
            //a command was sent, now get the output
            if(response.type != CommandHandler.ResponseType.valid){
                //send usage
                if(response.type == CommandHandler.ResponseType.manyArguments){
                    ctx.sendEmbed(false,":interrobang: **too many arguments**", "**usage:** " + prefix + response.command.text + " " + response.command.paramText);
                }else if(response.type == CommandHandler.ResponseType.fewArguments){
                    ctx.sendEmbed(false,":interrobang: **too few arguments**", "**usage:** " + prefix + response.command.text + " " + response.command.paramText);
                }else{ //unknown command
                    returnant = false;
                }
            }
        }
        return returnant;
    }
}