package com.kamikazejam.datastore.command.type;

import com.kamikazejam.datastore.DataStoreAPI;
import com.kamikazejam.datastore.base.Cache;
import org.bukkit.command.CommandSender;

import java.util.Collection;

public class TypeCache extends TypeAbstract<Cache<?,?>> {
    private static final TypeCache i = new TypeCache();
    public TypeCache() { super(Cache.class); }
    public static TypeCache get() {
        return i;
    }

    @Override
    public Cache<?,?> read(String str, CommandSender sender) throws KamiCommonException {
        Cache<?,?> cache = DataStoreAPI.getCache(str);
        if (cache == null) {
            throw new KamiCommonException().addMsg("<b>No Cache matching \"<p>%s<b>\". See `/datastore caches` for a list of them.", str);
        }
        return cache;
    }

    @Override
    public Collection<String> getTabList(CommandSender commandSender, String s) {
        return DataStoreAPI.getCaches().keySet()
                .stream()
                .filter(id -> id.toLowerCase().startsWith(s.toLowerCase()))
                .limit(20)
                .toList();
    }
}
