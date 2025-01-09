//package com.kamikazejam.datastore.command.sub;
//
//import com.kamikazejam.datastore.DataStoreAPI;
//import com.kamikazejam.datastore.base.Cache;
//import com.kamikazejam.datastore.util.Color;
//
//public class CmdCaches extends KamiCommand {
//    public CmdCaches() {
//        addAliases("caches");
//
//        addRequirements(RequirementHasPerm.get("datastore.command.caches"));
//    }
//
//    @Override
//    public void perform() {
//        sender.sendMessage(Color.t("&7***** &6Store Caches &7*****"));
//        for (Cache<?,?> c : DataStoreAPI.getCaches().values()) {
//            sender.sendMessage(Color.t("&7" + c.getName() + " - " + c.getLocalCacheSize() + " local objects"));
//        }
//    }
//}
