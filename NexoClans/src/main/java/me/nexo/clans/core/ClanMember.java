package me.nexo.clans.core;

import java.util.UUID;

// 🌟 En Java 21 podemos usar 'records' para crear objetos de datos en 1 sola línea
public record ClanMember(UUID uuid, String name, String role) {}