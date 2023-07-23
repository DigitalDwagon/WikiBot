package dev.digitaldragon.util;

import lombok.Getter;

@Getter
public class TaskWrapper {
    private static Object task;
    private static TaskType type;

    public TaskWrapper(Object task) {
        this.task = task;
        if (task instanceof CommandTask) {
            this.type = TaskType.COMMAND;
        }
        else if (task instanceof Runnable) {
            this.type = TaskType.RUNNABLE;
        }
    }
}
