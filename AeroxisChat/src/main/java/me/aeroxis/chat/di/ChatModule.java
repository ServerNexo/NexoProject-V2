package me.aeroxis.chat.di;

import com.google.inject.AbstractModule;
import me.aeroxis.chat.commands.ComandoChat;
import me.aeroxis.chat.AeroxisChatPlugin;
import me.aeroxis.chat.managers.AeroxisChatManager;

public class ChatModule extends AbstractModule {

    private final AeroxisChatPlugin plugin;

    public ChatModule(AeroxisChatPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    protected void configure() {
        bind(AeroxisChatPlugin.class).toInstance(plugin);
        bind(AeroxisChatManager.class).asEagerSingleton();
        bind(ComandoChat.class).asEagerSingleton();
    }
}