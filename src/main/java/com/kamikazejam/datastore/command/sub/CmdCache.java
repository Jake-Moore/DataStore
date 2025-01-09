package com.kamikazejam.datastore.command.sub;

import com.kamikazejam.datastore.base.Cache;
import com.kamikazejam.datastore.base.Store;
import com.kamikazejam.datastore.command.type.TypeCache;
import com.kamikazejam.kamicommon.command.KamiCommand;
import com.kamikazejam.kamicommon.command.Parameter;
import com.kamikazejam.kamicommon.command.requirement.RequirementHasPerm;
import com.kamikazejam.kamicommon.util.StringUtil;
import com.kamikazejam.kamicommon.util.exception.KamiCommonException;

import java.util.Arrays;
import java.util.List;

public class CmdCache extends KamiCommand {
    public CmdCache() {
        addAliases("cache");

        addRequirements(RequirementHasPerm.get("datastore.command.cache"));

        addParameter(Parameter.of(TypeCache.get()).name("cache").concatFromHere(true));
    }

    @Override
    public void perform() throws KamiCommonException {
        Cache<?,?> cache = readArg();
        List<String> localCacheKeys = getSomeKeyStrings(cache);

        sender.sendMessage(StringUtil.t("&7***** &6Store Cache: " + cache.getName() + " &7*****"));
        sender.sendMessage(StringUtil.t("&7" + cache.getLocalCacheSize() + " objects in local cache, first 10: " + Arrays.toString(localCacheKeys.toArray())));
        sender.sendMessage(StringUtil.t("&7Current State: " + (cache.isRunning() ? "&aRunning" : "&cNot running")));
    }

    @SuppressWarnings("unchecked")
    private <K, X extends Store<X, K>> List<String> getSomeKeyStrings(Cache<?, ?> c) {
        Cache<K, X> cache = (Cache<K, X>) c;
        return cache.getLocalStore().getKeyStrings(cache).stream()
                .limit(10)
                .toList();
    }
}
