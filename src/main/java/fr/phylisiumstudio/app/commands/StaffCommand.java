package fr.phylisiumstudio.app.commands;

import com.google.inject.Inject;
import fr.phylisiumstudio.app.menu.CampsiteResolver;
import fr.phylisiumstudio.app.menu.ChatMenu;
import fr.phylisiumstudio.logic.Campsite;
import fr.phylisiumstudio.logic.service.CampsiteService;
import fr.phylisiumstudio.logic.staff.Staff;
import fr.phylisiumstudio.logic.staff.StaffMarket;
import fr.phylisiumstudio.logic.staff.StaffRole;
import fr.phylisiumstudio.logic.staff.StaffService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.arguments.ArgumentType;

import java.util.UUID;

/**
 * {@code /staff} : gestion des ressources humaines. Recrutement de candidats,
 * affectation à un rôle, licenciement — le tout via boutons cliquables.
 */
public class StaffCommand extends Command {

    private final CampsiteService campsiteService;
    private final StaffService staffService;
    private final StaffMarket staffMarket;

    @Inject
    public StaffCommand(CampsiteService campsiteService, StaffService staffService, StaffMarket staffMarket) {
        super("staff");
        this.campsiteService = campsiteService;
        this.staffService = staffService;
        this.staffMarket = staffMarket;

        setDefaultExecutor((sender, ctx) -> showMenu(sender));

        var idArg = ArgumentType.Word("id");
        var roleArg = ArgumentType.Enum("role", StaffRole.class);

        addSyntax((sender, ctx) -> withCampsite(sender, campsite -> {
            recruit(campsite, UUID.fromString(ctx.get(idArg)));
            showMenu(sender);
        }), ArgumentType.Literal("recruit"), idArg);

        addSyntax((sender, ctx) -> withCampsite(sender, campsite -> {
            staffService.fire(campsite, UUID.fromString(ctx.get(idArg)));
            showMenu(sender);
        }), ArgumentType.Literal("fire"), idArg);

        addSyntax((sender, ctx) -> withCampsite(sender, campsite -> {
            assignRole(campsite, UUID.fromString(ctx.get(idArg)), ctx.get(roleArg));
            showMenu(sender);
        }), ArgumentType.Literal("assign"), idArg, roleArg);

        addSyntax((sender, ctx) -> withCampsite(sender, campsite -> {
            staffMarket.refresh(campsite.getUniqueID());
            showMenu(sender);
        }), ArgumentType.Literal("refresh"));
    }

    private void withCampsite(CommandSender sender, java.util.function.Consumer<Campsite> action) {
        var campsite = CampsiteResolver.resolve(sender, campsiteService);
        if (campsite != null) {
            action.accept(campsite);
        }
    }

    private void showMenu(CommandSender sender) {
        var campsite = CampsiteResolver.resolve(sender, campsiteService);
        if (campsite == null) return;

        var menu = ChatMenu.titled("Employés")
                .text("Solde : " + Math.round(campsite.getMoney()) + " $", NamedTextColor.GREEN)
                .blank()
                .text("— Effectif —", NamedTextColor.GOLD);

        if (campsite.getStaff().isEmpty()) {
            menu.text("Aucun employé.", NamedTextColor.GRAY);
        } else {
            for (var staff : campsite.getStaff()) {
                menu.line(describe(staff, true));
                menu.line(assignRow(staff));
            }
        }

        menu.blank().text("— Candidats (salaire quotidien) —", NamedTextColor.GOLD);
        for (var candidate : staffMarket.candidates(campsite.getUniqueID())) {
            menu.line(ChatMenu.row(
                    describe(candidate, false),
                    ChatMenu.button("Recruter", NamedTextColor.GREEN,
                            "/staff recruit " + candidate.getUniqueId(),
                            "Embaucher " + candidate.getName())));
        }

        menu.footer();
        menu.line(ChatMenu.row(
                ChatMenu.button("Rafraîchir", NamedTextColor.AQUA, "/staff refresh", "Nouveaux candidats"),
                ChatMenu.button("Retour", NamedTextColor.GRAY, "/camp", "Menu principal")));
        menu.send(sender);
    }

    private Component describe(Staff staff, boolean showRole) {
        var role = staff.getAssignedRole();
        var roleText = showRole
                ? " [" + (role != null ? role.name() + " " + pct(staff.skill(role)) : "inactif") + "]"
                : " (spéc. " + topRole(staff) + ")";
        return Component.text(staff.getName(), NamedTextColor.AQUA)
                .append(Component.text(roleText, NamedTextColor.GRAY))
                .append(Component.text(" — " + Math.round(staff.getDailySalary()) + " $/j", NamedTextColor.YELLOW));
    }

    private Component assignRow(Staff staff) {
        var builder = Component.text("  Affecter : ", NamedTextColor.DARK_GRAY);
        var row = ChatMenu.row(
                ChatMenu.button("Accueil", NamedTextColor.WHITE, "/staff assign " + staff.getUniqueId() + " RECEPTION", "Compétence " + pct(staff.skill(StaffRole.RECEPTION))),
                ChatMenu.button("Nettoyage", NamedTextColor.WHITE, "/staff assign " + staff.getUniqueId() + " CLEANING", "Compétence " + pct(staff.skill(StaffRole.CLEANING))),
                ChatMenu.button("Maintenance", NamedTextColor.WHITE, "/staff assign " + staff.getUniqueId() + " MAINTENANCE", "Compétence " + pct(staff.skill(StaffRole.MAINTENANCE))),
                ChatMenu.button("Finance", NamedTextColor.WHITE, "/staff assign " + staff.getUniqueId() + " FINANCE", "Compétence " + pct(staff.skill(StaffRole.FINANCE))),
                ChatMenu.button("Renvoyer", NamedTextColor.RED, "/staff fire " + staff.getUniqueId(), "Licencier " + staff.getName()));
        return builder.append(row);
    }

    private void recruit(Campsite campsite, UUID candidateId) {
        staffMarket.candidates(campsite.getUniqueID()).stream()
                .filter(s -> s.getUniqueId().equals(candidateId))
                .findFirst()
                .ifPresent(candidate -> {
                    staffService.hire(campsite, candidate);
                    staffMarket.remove(campsite.getUniqueID(), candidateId);
                });
    }

    private void assignRole(Campsite campsite, UUID staffId, StaffRole role) {
        campsite.getStaff().stream()
                .filter(s -> s.getUniqueId().equals(staffId))
                .findFirst()
                .ifPresent(s -> s.setAssignedRole(role));
    }

    private static String topRole(Staff staff) {
        StaffRole best = StaffRole.RECEPTION;
        for (var role : StaffRole.values()) {
            if (staff.skill(role) > staff.skill(best)) {
                best = role;
            }
        }
        return best.name() + " " + pct(staff.skill(best));
    }

    private static String pct(double skill) {
        return Math.round(skill * 100) + "%";
    }
}
