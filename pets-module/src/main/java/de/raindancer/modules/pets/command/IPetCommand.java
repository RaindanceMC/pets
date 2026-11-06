package de.raindancer.modules.pets.command;

import io.papermc.paper.command.brigadier.BasicCommand;

/** A command of this module: built at bootstrap, so it captures nothing and asks for the services when it runs. */
public interface IPetCommand extends BasicCommand {

    String describe();
}
