package fr.phylisiumstudio.app.commands;

import com.google.inject.Inject;
import fr.phylisiumstudio.app.menu.CampsiteResolver;
import fr.phylisiumstudio.app.menu.ChatMenu;
import fr.phylisiumstudio.app.menu.NpcLocator;
import fr.phylisiumstudio.logic.Campsite;
import fr.phylisiumstudio.logic.service.CampsiteService;
import fr.phylisiumstudio.logic.staff.Staff;
import fr.phylisiumstudio.logic.staff.StaffEntity;
import fr.phylisiumstudio.logic.staff.StaffMarket;
import fr.phylisiumstudio.logic.staff.StaffRole;
import fr.phylisiumstudio.logic.staff.StaffService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.entity.Player;
import net.minestom.server.command.builder.arguments.ArgumentType;

import java.util.UUID;
import java.util.function.Consumer;

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

        var activityIdArg = ArgumentType.Word("activityId");
        addSyntax((sender, ctx) -> withCampsite(sender, campsite -> {
            assignSupply(campsite, UUID.fromString(ctx.get(idArg)), UUID.fromString(ctx.get(activityIdArg)));
            showMenu(sender);
        }), ArgumentType.Literal("supply"), idArg, activityIdArg);

        addSyntax((sender, ctx) -> withCampsite(sender, campsite -> {
            staffMarket.refresh(campsite.getUniqueID());
            showMenu(sender);
        }), ArgumentType.Literal("refresh"));

        addSyntax((sender, ctx) -> locate(sender, UUID.fromString(ctx.get(idArg)), true),
                ArgumentType.Literal("tp"), idArg);
        addSyntax((sender, ctx) -> locate(sender, UUID.fromString(ctx.get(idArg)), false),
                ArgumentType.Literal("locate"), idArg);

        addSyntax((sender, ctx) -> wake(sender), ArgumentType.Literal("wake"));
    }

    /** Débloque tous les employés coincés de l'instance (téléportation à l'accueil). */
    private void wake(CommandSender sender) {
        if (!(sender instanceof Player player) || player.getInstance() == null) {
            return;
        }
        int count = 0;
        for (var entity : player.getInstance().getEntities()) {
            if (entity instanceof StaffEntity) {
                entity.getAcquirable().sync(e -> ((StaffEntity) e).unstick());
                count++;
            }
        }
        player.sendMessage(Component.text(count + " employé(s) débloqué(s).", NamedTextColor.GREEN));
    }

    /** Téléporte le joueur vers l'employé, ou le met en surbrillance. */
    private void locate(CommandSender sender, UUID staffId, boolean teleport) {
        if (!(sender instanceof Player player)) {
            return;
        }
        NpcLocator.findStaff(player, staffId).ifPresentOrElse(
                npc -> {
                    if (teleport) {
                        NpcLocator.teleportTo(player, npc);
                    } else {
                        NpcLocator.highlight(player, npc);
                    }
                },
                () -> player.sendMessage(Component.text(
                        "Employé introuvable dans le monde.", NamedTextColor.RED)));
    }

    private void withCampsite(CommandSender sender, Consumer<Campsite> action) {
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
                if (!campsite.getActivities().isEmpty()) {
                    menu.line(supplyRow(campsite, staff));
                }
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
                ChatMenu.button("Débloquer", NamedTextColor.GOLD, "/staff wake", "Débloquer les employés coincés"),
                ChatMenu.button("Retour", NamedTextColor.GRAY, "/camp", "Menu principal")));
        menu.send(sender);
    }

    private Component describe(Staff staff, boolean showRole) {
        var role = staff.getAssignedRole();
        var roleText = showRole
                ? " [" + (role != null ? role.displayName() + " " + pct(staff.skill(role)) : "inactif") + "]"
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
        var locate = ChatMenu.row(
                ChatMenu.button("TP", NamedTextColor.GREEN, "/staff tp " + staff.getUniqueId(), "Se téléporter sur l'employé"),
                ChatMenu.button("Localiser", NamedTextColor.AQUA, "/staff locate " + staff.getUniqueId(), "Le faire briller 5s"));
        return builder.append(row).append(Component.text("  ")).append(locate);
    }

    /** Boutons pour affecter l'employé au ravitaillement d'une des activités du camping. */
    private Component supplyRow(Campsite campsite, Staff staff) {
        var activities = campsite.getActivities();
        var parts = new java.util.ArrayList<Component>();
        for (var activity : activities) {
            boolean current = activity.getUniqueID().equals(staff.getAssignedActivityId());
            parts.add(ChatMenu.button(activity.getType().displayName(),
                    current ? NamedTextColor.GREEN : NamedTextColor.WHITE,
                    "/staff supply " + staff.getUniqueId() + " " + activity.getUniqueID(),
                    "Ravitailler " + activity.getType().displayName() + " (compétence "
                            + pct(staff.skill(StaffRole.SUPPLY)) + ")"));
        }
        return Component.text("  Ravit. : ", NamedTextColor.DARK_GRAY)
                .append(ChatMenu.row(parts.toArray(new Component[0])));
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
                .ifPresent(s -> {
                    s.setAssignedRole(role);
                    // Changer pour un autre rôle détache l'activité de ravitaillement.
                    if (role != StaffRole.SUPPLY) {
                        s.setAssignedActivityId(null);
                    }
                });
    }

    /** Affecte l'employé au ravitaillement d'une activité précise. */
    private void assignSupply(Campsite campsite, UUID staffId, UUID activityId) {
        boolean activityExists = campsite.getActivities().stream()
                .anyMatch(a -> a.getUniqueID().equals(activityId));
        if (!activityExists) {
            return;
        }
        campsite.getStaff().stream()
                .filter(s -> s.getUniqueId().equals(staffId))
                .findFirst()
                .ifPresent(s -> {
                    s.setAssignedRole(StaffRole.SUPPLY);
                    s.setAssignedActivityId(activityId);
                });
    }

    private static String topRole(Staff staff) {
        StaffRole best = StaffRole.RECEPTION;
        for (var role : StaffRole.values()) {
            if (staff.skill(role) > staff.skill(best)) {
                best = role;
            }
        }
        return best.displayName() + " " + pct(staff.skill(best));
    }

    private static String pct(double skill) {
        return Math.round(skill * 100) + "%";
    }
}
