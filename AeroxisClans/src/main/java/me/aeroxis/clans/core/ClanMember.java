package me.aeroxis.clans.core;

import java.util.UUID;

/**
 * 👥 AeroxisClans - Miembro de Clan (Modelo Enterprise)
 * Data carrier inmutable. 100% Thread-Safe por naturaleza.
 */
public record ClanMember(UUID uuid, String name, String role) {}