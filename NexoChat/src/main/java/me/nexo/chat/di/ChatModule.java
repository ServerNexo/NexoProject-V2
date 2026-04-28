package me.nexo.chat.di;

import com.google.inject.AbstractModule;
import me.nexo.chat.NexoChatPlugin;
import me.nexo.chat.managers.NexoChatManager;

public class ChatModule extends AbstractModule {

    private final NexoChatPlugin plugin;

    public ChatModule(NexoChatPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    protected void configure() {
        bind(NexoChatPlugin.class).toInstance(plugin);
        bind(NexoChatManager.class).asEagerSingleton();
        bind(me.nexo.chat.commands.ComandoChat.class).asEagerSingleton();
    }
}