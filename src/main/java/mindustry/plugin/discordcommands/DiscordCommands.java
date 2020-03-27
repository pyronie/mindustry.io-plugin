package mindustry.plugin.discordcommands;

import arc.util.CommandHandler;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import static mindustry.plugin.ioMain.*;

/** Represents a registry of commands */
public class DiscordCommands extends ListenerAdapter {
    public DiscordCommands() {
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        Context ctx = new Context(event);
        //check if it's a command
        CommandHandler.CommandResponse response = bt.iohandler.handleMessage(event.getMessage().getContentRaw(), ctx);

        if (response.type != CommandHandler.ResponseType.noCommand) {
            //a command was sent, now get the output
            if(response.type != CommandHandler.ResponseType.valid){
                //send usage
                String description = "**usage:** " + prefix + response.command.text + " " + response.command.paramText;

                if(response.type == CommandHandler.ResponseType.manyArguments){
                    ctx.sendEmbed(false,":interrobang: **too many arguments**", description);
                }else if(response.type == CommandHandler.ResponseType.fewArguments){
                    ctx.sendEmbed(false,":interrobang: **too few arguments**", description);
                }else{ //unknown command
                    ctx.sendEmbed(false,":interrobang: **unknown command**", "**check** " + prefix + "help");
                }
            }
        }
    }

}