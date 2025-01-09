//package com.kamikazejam.datastore.command.sub;
//
//import com.kamikazejam.datastore.DataStoreSource;
//import com.kamikazejam.datastore.util.Color;
//
//public class CmdInfo extends KamiCommand {
//    public CmdInfo() {
//        addAliases("info", "id");
//
//        addRequirements(RequirementHasPerm.get("datastore.command.info"));
//    }
//
//    @Override
//    public void perform() {
//        sender.sendMessage(Color.t("&7--- &6DataStore Information&7---"));
//        sender.sendMessage(Color.t("&7Database Prefix:"));
//        sender.sendMessage(Color.t("  &6" + DataStoreSource.getStoreDbPrefix()));
//        sender.sendMessage(Color.t("&7Storage Service:"));
//        sender.sendMessage(Color.t("  &6Name: " + DataStoreSource.getStorageMode().name()));
//        String r = (DataStoreSource.getStorageService().canCache()) ? "&aYes" : "&cNo";
//        sender.sendMessage(Color.t("  &6Ready: " + r));
//    }
//}
