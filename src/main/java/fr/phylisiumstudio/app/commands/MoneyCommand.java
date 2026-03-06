package fr.phylisiumstudio.app.commands;

import com.google.inject.Inject;
import fr.phylisiumstudio.logic.service.CampsiteService;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.entity.Player;

public class MoneyCommand extends Command {

    private final CampsiteService campsiteService;

    @Inject
    public MoneyCommand(CampsiteService campsiteService) {
        super("money");
        this.campsiteService = campsiteService;

        setDefaultExecutor(this::Executor);
    }

    private void Executor(CommandSender sender, CommandContext ctx) {
        if(sender instanceof Player player) {
            var campsite = campsiteService.getCampsiteByOwner(player.getUuid());
            if(campsite.isPresent()) {
                var money = campsite.get().getMoney();
                sender.sendMessage("You have " + money + " money.");
            } else {
                sender.sendMessage("You don't have a campsite.");
            }
        } else {
            sender.sendMessage("This command can only be used by players.");
        }
    }
}
