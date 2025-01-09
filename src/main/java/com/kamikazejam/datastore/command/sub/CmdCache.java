package com.kamikazejam.datastore.command.sub;

import com.kamikazejam.datastore.base.Cache;
import com.kamikazejam.datastore.base.Store;
import com.kamikazejam.datastore.command.SubCommand;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

public class CmdCache extends SubCommand {
    public CmdCache() {}

    @Override
    public @NotNull List<String> getAliases() { return List.of("cache"); }
    @Override
    public @NotNull String getName() { return "cache"; }
    @Override
    public @Nullable String getPermission() { return "datastore.command.cache"; }
    @Override
    public @NotNull String getArgsDescription() { return "<cache>"; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission(this.getPermission())) {
            this.sendNoPerm(sender);
            return true;
        }

        // TODO
        Bukkit.getLogger().info("Provided Sub Args for Cache: " + Arrays.toString(args));

        return true;
    }

//    @Override
//    public void perform() throws KamiCommonException {
//        Cache<?,?> cache = readArg();
//        List<String> localCacheKeys = getSomeKeyStrings(cache);
//
//        sender.sendMessage(Color.t("&7***** &6Store Cache: " + cache.getName() + " &7*****"));
//        sender.sendMessage(Color.t("&7" + cache.getLocalCacheSize() + " objects in local cache, first 10: " + Arrays.toString(localCacheKeys.toArray())));
//        sender.sendMessage(Color.t("&7Current State: " + (cache.isRunning() ? "&aRunning" : "&cNot running")));
//    }

    @SuppressWarnings("unchecked")
    private <K, X extends Store<X, K>> List<String> getSomeKeyStrings(Cache<?, ?> c) {
        Cache<K, X> cache = (Cache<K, X>) c;
        return cache.getLocalStore().getKeyStrings(cache).stream()
                .limit(10)
                .toList();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        return List.of();
    }
}
