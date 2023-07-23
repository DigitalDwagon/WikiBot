package dev.digitaldragon.util;

import lombok.Getter;
import lombok.Setter;

/**
 * CommandTask is a class that represents command tasks.
 * It holds the name, command, priority, success code and a flag indicating if it is always successful.
 * It also overrides compareTo method for sorting based on priority.
 */
public class CommandTask implements Comparable<CommandTask> {
    @Getter
    @Setter
    private String command;

    @Getter
    @Setter
    private int priority;

    @Getter
    @Setter
    private String name;

    @Setter
    private boolean alwaysSuccessful;

    @Getter
    @Setter
    private int successCode = 0;

    /**
     * Constructor.
     *
     * @param command  the command of the task
     * @param priority  the priority of the task
     * @param name  the name of the task
     */
    public CommandTask(String command, int priority, String name) {
        this.command = command;
        this.priority = priority;
        this.name = name;
    }

    /**
     * Compare CommandTask based on their priority and name.
     *
     * @param other  the other CommandTask to compare with
     * @return a negative integer, zero, or a positive integer if this CommandTask is less than, equal to, or greater than the other CommandTask.
     */
    @Override
    public int compareTo(CommandTask other) {
        return Integer.compare(this.priority, other.priority);
    }

    /**
     * Check if task is successful based on the exit code.
     *
     * @param exitCode  the exit code
     * @return true if task is always successful or exit code is equal to success code, false otherwise
     */
    public boolean taskSuccess(int exitCode) {
        return alwaysSuccessful || exitCode == successCode;
    }
}
