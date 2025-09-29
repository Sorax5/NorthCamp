package fr.phylisiumstudio.app.commands;

import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.CommandContext;

public class ShutdownCommand extends Command {
    public ShutdownCommand() {
        super("shutdown");

        setDefaultExecutor(this::Executor);
    }

    private void Executor(CommandSender sender, CommandContext ctx) {
        sender.sendMessage("Shutting down the server...");
        sender.sendMessage("Goodbye!");
        sender.sendMessage("§c§lThe server is shutting down in 5 seconds. Please disconnect.");
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        sender.sendMessage("§c§lServer is now shutting down.");
        System.exit(0);
    }

}
