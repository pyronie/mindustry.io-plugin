package mindustry.plugin.discordcommands;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import arc.util.CommandHandler;
import arc.util.Log;
import mindustry.gen.Call;
import mindustry.plugin.ioMain;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.listener.message.MessageCreateListener;

import static mindustry.Vars.*;
import static mindustry.plugin.ioMain.*;

/** Represents a registry of commands */
public class DiscordCommands implements MessageCreateListener {
    private HashMap<String, Command> registry = new HashMap<>();
    public DiscordCommands() {
        // stuff
    }
    /**
     * Register a command in the CommandRegistry
     * @param c The command
     */
    public void registerCommand(Command c) {
        registry.put(c.name.toLowerCase(), c);
    }
    // you can override the name of the command manually, for example for aliases
    /**
     * Register a command in the CommandRegistry
     * @param forcedName Register the command under another name
     * @param c The command to register
     */
    public void registerCommand(String forcedName, Command c) {
        registry.put(forcedName.toLowerCase(), c);
    }
    /**
     * Parse and run a command
     * @param event Source event associated with the message
     */
    public void onMessageCreate(MessageCreateEvent event) {

        Log.info(event.getMessageContent());
        //check if it's a command
        CommandHandler.CommandResponse response = bt.iohandler.handleMessage(event.getMessageContent(), new Context(event));

        if (response.type != CommandHandler.ResponseType.noCommand) {
            //a command was sent, now get the output
            if(response.type != CommandHandler.ResponseType.valid){
                String text;

                //send usage
                if(response.type == CommandHandler.ResponseType.manyArguments){
                    text = "[scarlet]Too many arguments. Usage:[lightgray] " + response.command.text + "[gray] " + response.command.paramText;
                }else if(response.type == CommandHandler.ResponseType.fewArguments){
                    text = "[scarlet]Too few arguments. Usage:[lightgray] " + response.command.text + "[gray] " + response.command.paramText;
                }else{ //unknown command
                    text = "[scarlet]Unknown command. Check [lightgray]/help[scarlet].";
                }

                Log.info(text);
            }
        }

        /*String message = event.getMessageContent();
        if (!message.startsWith(ioMain.prefix)) return;
        String[] args = message.split(" ");
        int commandLength = args[0].length();
        args[0] = args[0].substring(ioMain.prefix.length());
        String name = args[0];

        String newMessage = null;
        if (args.length > 1) newMessage = message.substring(commandLength + 1);
        runCommand(name, new Context(event, args, newMessage));*/
    }

    /**
     * Get a command by name
     * @param name
     * @return
     */
    public Command getCommand(String name) {
        return registry.get(name.toLowerCase());
    }
    /**
     * Get all commands in the registry
     * @return
     */
    public Collection<Command> getAllCommands() {
        return registry.values();
    }
    /**
     * Check if a command exists in the registry
     * @param name
     * @return
     */
    public boolean isCommand(String name) {
        return registry.containsKey(name.toLowerCase());
    }
}