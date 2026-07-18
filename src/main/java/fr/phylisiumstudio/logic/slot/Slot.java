package fr.phylisiumstudio.logic.slot;

import org.joml.Vector3d;

/**
 * Emplacement disponible sur la carte, découvert via les marqueurs. Tant qu'il
 * n'est pas acheté et défini, il ne représente ni un camping ni une activité —
 * juste une position constructible.
 */
public record Slot(int index, SlotKind kind, Vector3d position) {
}
