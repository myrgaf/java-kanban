package ru.yandex.practicum.manager;

import ru.yandex.practicum.models.*;

import java.util.*;

public class InMemoryHistoryManager implements HistoryManager {
    private static final int HISTORY_LIMIT = 10;
    private final List<Task> history;

    public InMemoryHistoryManager() {
        this.history = new ArrayList<>();
    }

    @Override
    public void add(Task task) {
        if (task == null) return;

        history.removeIf(t -> t.getId() == task.getId());

        Task copy;
        if (task instanceof Subtask) {
            Subtask subtask = (Subtask) task;
            copy = new Subtask(subtask.getTitle(), subtask.getDescription(), subtask.getStatus(), subtask.getEpicId());
            copy.setId(subtask.getId());
        } else if (task instanceof Epic) {
            Epic epic = (Epic) task;
            copy = new Epic(epic.getTitle(), epic.getDescription(), epic.getStatus());
            copy.setId(epic.getId());
            copy.setStatus(epic.getStatus());
        } else {
            copy = new Task(task.getTitle(), task.getDescription(), task.getStatus());
            copy.setId(task.getId());
            copy.setStatus(task.getStatus());
        }

        history.add(copy);

        if (history.size() > 10) {
            history.removeFirst();
        }
    }

    @Override
    public List<Task> getHistory() {
        return new ArrayList<>(history);
    }
}