package com.kamikazejam.datastore.command;

import com.kamikazejam.datastore.command.sub.CmdCache;
import com.kamikazejam.datastore.command.sub.CmdCaches;
import com.kamikazejam.datastore.command.sub.CmdDatabases;
import com.kamikazejam.datastore.command.sub.CmdInfo;
import com.kamikazejam.kamicommon.command.KamiCommand;
import com.kamikazejam.kamicommon.command.requirement.RequirementHasPerm;

public class DataStoreCommand extends KamiCommand {
    public DataStoreCommand() {
        addAliases("datastore");

        addRequirements(RequirementHasPerm.get("datastore.command.help"));

        addChild(new CmdCache());
        addChild(new CmdCaches());
        addChild(new CmdDatabases());
        addChild(new CmdInfo());
    }
}
