package com.kamikazejam.datastore.command.sub;

import com.kamikazejam.datastore.DataStoreAPI;
import com.kamikazejam.datastore.base.Cache;
import com.kamikazejam.datastore.base.Store;
import com.kamikazejam.datastore.command.SubCommand;
import com.kamikazejam.datastore.util.Color;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

public class CmdCache extends SubCommand {
    public CmdCache() {}

    @Override
    public @NotNull String getName() { return "cache"; }
    @Override
    public @Nullable String getPermission() { return "datastore.command.cache"; }
    @Override
    public @NotNull String getArgsDescription() { return "<cache>"; }

    @Override
    public void execute(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length < 1) {
            this.sendUsage(sender);
            return;
        }

        String cacheName = args[0];
        Cache<?,?> cache = DataStoreAPI.getCache(cacheName);
        if (cache == null) {
            sender.sendMessage(Color.t("&cNo Cache matching \"" + cacheName + "\". See `/datastore caches` for a list of them."));
            return;
        }

        // Perform Command now that we have args
        List<String> localCacheKeys = getSomeKeyStrings(cache);
        sender.sendMessage(Color.t("&7***** &6Store Cache: " + cache.getName() + " &7*****"));
        sender.sendMessage(Color.t("&7" + cache.getLocalCacheSize() + " objects in local cache, first 10: " + Arrays.toString(localCacheKeys.toArray())));
        sender.sendMessage(Color.t("&7Current State: " + (cache.isRunning() ? "&aRunning" : "&cNot running")));
    }

    @Override
    public @NotNull List<String> getTabCompletions(@NotNull CommandSender sender, @NotNull String[] args) {
        String s = args.length == 0 ? "" : args[args.length - 1];
        return DataStoreAPI.getCaches().keySet()
                .stream()
                .filter(id -> id.toLowerCase().startsWith(s.toLowerCase()))
                .limit(20)
                .toList();
    }

    @SuppressWarnings("unchecked")
    private <K, X extends Store<X, K>> List<String> getSomeKeyStrings(Cache<?, ?> c) {
        Cache<K, X> cache = (Cache<K, X>) c;
        return cache.getLocalStore().getKeyStrings(cache).stream()
                .limit(10)
                .toList();
    }
}
