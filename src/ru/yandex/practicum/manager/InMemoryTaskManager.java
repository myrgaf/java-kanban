package ru.yandex.practicum.manager;// src/ru.yandex.practicum.manager.TaskManager.java

import ru.yandex.practicum.models.*;
import java.util.*;

public class InMemoryTaskManager implements TaskManager{
    private final Map<Integer, Task> tasks;
    private final Map<Integer, Epic> epics;
    private final Map<Integer, Subtask> subtasks;
    private int nextId;
    private final HistoryManager historyManager;

    public InMemoryTaskManager(HistoryManager historyManager) {
        this.historyManager = historyManager;
        this.tasks = new HashMap<>();
        this.epics = new HashMap<>();
        this.subtasks = new HashMap<>();
        this.nextId = 1;
    }

    public InMemoryTaskManager() {
        this(new InMemoryHistoryManager());
    }

    private int generateId() {
        return nextId++;
    }

    // === ru.yandex.practicum.models.Task ===

    @Override
    public List<Task> getAllTasks() {
        List<Task> result = new ArrayList<>();
        for (Task task : tasks.values()) {
            if (task instanceof Task && !(task instanceof Epic) && !(task instanceof Subtask)) {
                result.add(task);
            }
        }
        return result;
    }

    @Override
    public void deleteAllTasks() {
        tasks.values().removeIf(task -> task instanceof Task && !(task instanceof Epic || task instanceof Subtask));
    }

    @Override
    public Task getTask(int id) {
        Task task = tasks.get(id);
        if (task != null && task instanceof Task && !(task instanceof Epic || task instanceof Subtask)) {
            historyManager.add(task);
            return task;
        }
        return null;
    }

    @Override
    public Task createTask(Task task) {
        Task copy = new Task(task.getTitle(), task.getDescription(), task.getStatus());
        if (task.getId() == 0) {
            copy.setId(generateId());
        } else {
            copy.setId(task.getId());
        }
        tasks.put(copy.getId(), copy);
        return copy;
    }

    @Override
    public Task updateTask(Task updatedTask) {
        if (tasks.containsKey(updatedTask.getId())) {
            Task existing = tasks.get(updatedTask.getId());
            if (existing != null && !(existing instanceof Epic || existing instanceof Subtask)) {
                tasks.put(updatedTask.getId(), updatedTask);
                return updatedTask;
            }
        }
        return null;
    }

    @Override
    public boolean deleteTask(int id) {
        Task task = tasks.get(id);
        if (task != null && !(task instanceof Epic || task instanceof Subtask)) {
            return tasks.remove(id) != null;
        }
        return false;
    }

    // === ru.yandex.practicum.models.Epic ===

    @Override
    public List<Epic> getAllEpics() {
        return new ArrayList<>(epics.values());
    }

    @Override
    public void deleteAllEpics() {
        for (Epic epic : epics.values()) {
            epic.clearSubtasks();
        }
        epics.clear();
        subtasks.clear();
        tasks.entrySet().removeIf(entry -> entry.getValue() instanceof Epic || entry.getValue() instanceof Subtask);
    }

    @Override
    public Epic getEpic(int id) {
        Epic epic = epics.get(id);
        if (epic != null) {
            historyManager.add(epic);
        }
        return epic;
    }

    @Override
    public Epic createEpic(Epic epic) {
        Epic copy = new Epic(epic.getTitle(), epic.getDescription(), epic.getStatus());
        if (epic.getId() == 0) {
            copy.setId(generateId());
        } else {
            copy.setId(epic.getId());
        }
        epics.put(copy.getId(), copy);
        tasks.put(copy.getId(), copy);
        return copy;
    }

    @Override
    public Epic updateEpic(Epic updatedEpic) {
        if (epics.containsKey(updatedEpic.getId())) {
            Epic epic = epics.get(updatedEpic.getId());
            epic.setTitle(updatedEpic.getTitle());
            epic.setDescription(updatedEpic.getDescription());
            // Статус обновляется автоматически
            updateEpicStatus(epic);
            return epic;
        }
        return null;
    }

    @Override
    public boolean deleteEpic(int id) {
        Epic epic = epics.remove(id);
        if (epic != null) {
            tasks.remove(id);

            // Удаляем все подзадачи эпика
            List<Integer> subtaskIdsToRemove = new ArrayList<>(epic.getSubtaskIds());
            for (Integer subId : subtaskIdsToRemove) {
                subtasks.remove(subId);
                tasks.remove(subId);
            }

            epic.clearSubtasks();
            return true;
        }
        return false;
    }

    // === ru.yandex.practicum.models.Subtask ===

    @Override
    public List<Subtask> getAllSubtasks() {
        return new ArrayList<>(subtasks.values());
    }

    @Override
    public void deleteAllSubtasks() {
        for (Epic epic : epics.values()) {
            epic.clearSubtasks();
            updateEpicStatus(epic);
        }
        subtasks.clear();
        tasks.values().removeIf(task -> task instanceof Subtask);
    }

    @Override
    public Subtask getSubtask(int id) {
        Subtask subtask = subtasks.get(id);
        if (subtask != null) {
            historyManager.add(subtask);
        }
        return subtasks.get(id);
    }

    @Override
    public Subtask createSubtask(Subtask subtask) {
        if (!epics.containsKey(subtask.getEpicId())) {
            return null;
        }
        if (subtask.getEpicId() == subtask.getId()) {
            return null;
        }

        Subtask copy = new Subtask(
                subtask.getTitle(),
                subtask.getDescription(),
                subtask.getStatus(),
                subtask.getEpicId()
        );
        if (subtask.getId() == 0) {
            copy.setId(generateId());
        } else {
            copy.setId(subtask.getId());
        }

        subtasks.put(copy.getId(), copy);
        tasks.put(copy.getId(), copy);

        Epic epic = epics.get(copy.getEpicId());
        epic.addSubtask(copy.getId());
        updateEpicStatus(epic);

        return copy;
    }

    @Override
    public Subtask updateSubtask(Subtask updatedSubtask) {
        Subtask subtask = subtasks.get(updatedSubtask.getId());
        if (subtask == null) return null;

        int oldEpicId = subtask.getEpicId();
        int newEpicId = updatedSubtask.getEpicId();

        subtask.setTitle(updatedSubtask.getTitle());
        subtask.setDescription(updatedSubtask.getDescription());
        subtask.setStatus(updatedSubtask.getStatus());

        if (oldEpicId != newEpicId) {
            Epic oldEpic = epics.get(oldEpicId);
            if (oldEpic != null) {
                oldEpic.removeSubtask(subtask.getId());
                updateEpicStatus(oldEpic);
            }

            Epic newEpic = epics.get(newEpicId);
            if (newEpic != null) {
                newEpic.addSubtask(subtask.getId());
                updateEpicStatus(newEpic);
            }
            subtask.setEpicId(newEpicId);
        } else {
            Epic epic = epics.get(oldEpicId);
            if (epic != null) {
                updateEpicStatus(epic);
            }
        }

        return subtask;
    }

    @Override
    public boolean deleteSubtask(int id) {
        Subtask subtask = subtasks.remove(id);
        if (subtask != null) {
            tasks.remove(id);
            Epic epic = epics.get(subtask.getEpicId());
            if (epic != null) {
                epic.removeSubtask(id);
                updateEpicStatus(epic);
            }
            return true;
        }
        return false;
    }


    @Override
    public List<Subtask> getEpicSubtasks(int epicId) {
        Epic epic = epics.get(epicId);
        if (epic == null) return new ArrayList<>();

        List<Subtask> result = new ArrayList<>();
        for (Integer subId : epic.getSubtaskIds()) {
            Subtask subtask = subtasks.get(subId);
            if (subtask != null) {
                result.add(subtask);
            }
        }
        return result;
    }

    private void updateEpicStatus(Epic epic) {
        List<Subtask> subtasks = getEpicSubtasks(epic.getId());

        if (subtasks.isEmpty()) {
            epic.setStatus(Status.NEW);
            return;
        }

        boolean allNew = true;
        boolean allDone = true;

        for (Subtask subtask : subtasks) {
            if (subtask.getStatus() != Status.NEW) allNew = false;
            if (subtask.getStatus() != Status.DONE) allDone = false;
        }

        if (allDone) {
            epic.setStatus(Status.DONE);
        } else if (allNew) {
            epic.setStatus(Status.NEW);
        } else {
            epic.setStatus(Status.IN_PROGRESS);
        }
    }

    @Override
    public List<Task> getHistory() {
        return historyManager.getHistory();
    }
}