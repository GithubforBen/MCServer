package de.hems;

public interface Command {
    public CommandType getType();
    public String getCommand();
}enum CommandType {
    STOP
}
