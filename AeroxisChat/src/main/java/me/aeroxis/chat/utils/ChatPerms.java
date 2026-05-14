package me.aeroxis.chat.utils;

public final class ChatPerms {
    
    // 🛡️ Permisos de Administración
    public static final String ADMIN = "nexochat.admin";
    public static final String STAFF_CHAT = "nexochat.staffchat";
    public static final String MUTE = "nexochat.mute"; // 🌟 ¡Añadido para el sistema de muteos!
    
    // 💬 Permisos de Chat y Bypass
    public static final String BYPASS_SPAM = "nexochat.spam.bypass";
    public static final String TAGS = "nexochat.tags";
    
    // 🎨 Permisos de Cosméticos
    public static final String HEX_COLORS = "nexochat.hex";
    public static final String RGB_COLORS = "nexochat.rgb";

    // Constructor privado para que nadie pueda instanciar esta clase por error
    private ChatPerms() {} 
}