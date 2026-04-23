package me.nexo.clans.core;

import java.util.UUID;

/**
 * 👥 NexoClans - Miembro de Clan (Modelo Enterprise)
 * Data carrier inmutable. 100% Thread-Safe por naturaleza.
 */
public record ClanMember(UUID uuid, String name, String role) {}