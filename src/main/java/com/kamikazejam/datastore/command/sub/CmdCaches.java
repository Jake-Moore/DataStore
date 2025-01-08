package com.kamikazejam.datastore.command.sub;

import com.kamikazejam.datastore.DataStoreAPI;
import com.kamikazejam.datastore.base.Cache;
import com.kamikazejam.kamicommon.command.KamiCommand;
import com.kamikazejam.kamicommon.command.requirement.RequirementHasPerm;
import com.kamikazejam.kamicommon.util.StringUtil;

public class CmdCaches extends KamiCommand {
    public CmdCaches() {
        addAliases("caches");

        addRequirements(RequirementHasPerm.get("datastore.command.caches"));
    }

    @Override
    public void perform() {
        sender.sendMessage(StringUtil.t("&7***** &6Store Caches &7*****"));
        for (Cache<?,?> c : DataStoreAPI.getCaches().values()) {
            sender.sendMessage(StringUtil.t("&7" + c.getName() + " - " + c.getLocalCacheSize() + " local objects"));
        }
    }
}
