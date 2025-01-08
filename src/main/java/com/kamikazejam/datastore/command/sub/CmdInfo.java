package com.kamikazejam.datastore.command.sub;

import com.kamikazejam.datastore.DataStoreSource;
import com.kamikazejam.kamicommon.command.KamiCommand;
import com.kamikazejam.kamicommon.command.requirement.RequirementHasPerm;
import com.kamikazejam.kamicommon.util.StringUtil;

public class CmdInfo extends KamiCommand {
    public CmdInfo() {
        addAliases("info", "id");

        addRequirements(RequirementHasPerm.get("datastore.command.info"));
    }

    @Override
    public void perform() {
        sender.sendMessage(StringUtil.t("&7--- &6DataStore Information&7---"));
        sender.sendMessage(StringUtil.t("&7Database Prefix:"));
        sender.sendMessage(StringUtil.t("  &6" + DataStoreSource.getStoreDbPrefix()));
        sender.sendMessage(StringUtil.t("&7Storage Service:"));
        sender.sendMessage(StringUtil.t("  &6Name: " + DataStoreSource.getStorageMode().name()));
        String r = (DataStoreSource.getStorageService().canCache()) ? "&aYes" : "&cNo";
        sender.sendMessage(StringUtil.t("  &6Ready: " + r));
    }
}
