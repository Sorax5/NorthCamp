package fr.phylisiumstudio.app.menu;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minestom.server.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

/**
 * Petit constructeur de menus dans le tchat à base de composants Adventure.
 *
 * <p>Les « boutons » sont des textes cliquables : au clic, ils exécutent une
 * commande ({@link #button}) ou la pré-remplissent dans le champ de saisie
 * ({@link #suggestButton}). Cela offre une interface interactive sans inventaire.
 */
public final class ChatMenu {

    private static final Component SEPARATOR =
            Component.text("──────────────────────────", NamedTextColor.DARK_GRAY);

    private final List<Component> lines = new ArrayList<>();

    private ChatMenu() {
    }

    public static ChatMenu titled(String title) {
        var menu = new ChatMenu();
        menu.lines.add(SEPARATOR);
        menu.lines.add(Component.text(title, NamedTextColor.GOLD, TextDecoration.BOLD));
        menu.lines.add(SEPARATOR);
        return menu;
    }

    public ChatMenu line(Component component) {
        lines.add(component);
        return this;
    }

    public ChatMenu text(String text, NamedTextColor color) {
        lines.add(Component.text(text, color));
        return this;
    }

    public ChatMenu blank() {
        lines.add(Component.empty());
        return this;
    }

    public ChatMenu footer() {
        lines.add(SEPARATOR);
        return this;
    }

    public void send(CommandSender sender) {
        for (var line : lines) {
            sender.sendMessage(line);
        }
    }

    // ── Boutons ───────────────────────────────────────────────────────────────

    /** Bouton exécutant une commande au clic. */
    public static Component button(String label, NamedTextColor color, String command, String hover) {
        return Component.text("[" + label + "]", color)
                .clickEvent(ClickEvent.runCommand(command))
                .hoverEvent(HoverEvent.showText(Component.text(hover)));
    }

    /** Bouton pré-remplissant une commande dans la saisie (pour compléter un argument). */
    public static Component suggestButton(String label, NamedTextColor color, String command, String hover) {
        return Component.text("[" + label + "]", color)
                .clickEvent(ClickEvent.suggestCommand(command))
                .hoverEvent(HoverEvent.showText(Component.text(hover)));
    }

    /** Assemble plusieurs composants sur une ligne, séparés par des espaces. */
    public static Component row(Component... parts) {
        var builder = Component.text();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                builder.append(Component.space());
            }
            builder.append(parts[i]);
        }
        return builder.build();
    }
}
