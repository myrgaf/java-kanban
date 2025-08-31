package ru.yandex.practicum.manager;// src/ru.yandex.practicum.manager.TaskManager.java
import ru.yandex.practicum.models.Epic;
import ru.yandex.practicum.models.Status;
import ru.yandex.practicum.models.Subtask;
import ru.yandex.practicum.models.Task;

import java.util.*;

public class TaskManager {
    private final Map<Integer, Task> tasks;
    private final Map<Integer, Epic> epics;
    private final Map<Integer, Subtask> subtasks;
    private int nextId;

    public TaskManager() {
        this.tasks = new HashMap<>();
        this.epics = new HashMap<>();
        this.subtasks = new HashMap<>();
        this.nextId = 1;
    }

    private int generateId() {
        return nextId++;
    }

    // === ru.yandex.practicum.models.Task ===

    public List<Task> getAllTasks() {
        List<Task> result = new ArrayList<>();
        for (Task task : tasks.values()) {
            if (task instanceof Task && !(task instanceof Epic) && !(task instanceof Subtask)) {
                result.add(task);
            }
        }
        return result;
    }

    public void deleteAllTasks() {
        tasks.values().removeIf(task -> task instanceof Task && !(task instanceof Epic || task instanceof Subtask));
    }

    public Task getTaskById(int id) {
        Task task = tasks.get(id);
        if (task != null && task instanceof Task && !(task instanceof Epic || task instanceof Subtask)) {
            return task;
        }
        return null;
    }

    public Task createTask(Task task) {
        task.setId(generateId());
        tasks.put(task.getId(), task);
        return task;
    }

    public Task updateTask(Task updatedTask) {
        if (tasks.containsKey(updatedTask.getId())) {
            Task existing = tasks.get(updatedTask.getId());
            if (existing instanceof Task && !(existing instanceof Epic || existing instanceof Subtask)) {
                tasks.put(updatedTask.getId(), updatedTask);
                return updatedTask;
            }
        }
        return null;
    }

    public boolean deleteTaskById(int id) {
        Task task = tasks.get(id);
        if (task != null && !(task instanceof Epic || task instanceof Subtask)) {
            return tasks.remove(id) != null;
        }
        return false;
    }

    // === ru.yandex.practicum.models.Epic ===

    public List<Epic> getAllEpics() {
        return new ArrayList<>(epics.values());
    }

    public void deleteAllEpics() {
        for (Epic epic : epics.values()) {
            epic.clearSubtasks();
        }
        epics.clear();
        subtasks.clear();
        tasks.entrySet().removeIf(entry -> entry.getValue() instanceof Epic || entry.getValue() instanceof Subtask);
    }

    public Epic getEpicById(int id) {
        return epics.get(id);
    }

    public Epic createEpic(Epic epic) {
        epic.setId(generateId());
        epics.put(epic.getId(), epic);
        tasks.put(epic.getId(), epic);
        return epic;
    }

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

    public boolean deleteEpicById(int id) {
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

    public List<Subtask> getAllSubtasks() {
        return new ArrayList<>(subtasks.values());
    }

    public void deleteAllSubtasks() {
        for (Epic epic : epics.values()) {
            epic.clearSubtasks();
            updateEpicStatus(epic);
        }
        subtasks.clear();
        tasks.values().removeIf(task -> task instanceof Subtask);
    }

    public Subtask getSubtaskById(int id) {
        return subtasks.get(id);
    }

    public Subtask createSubtask(Subtask subtask) {
        if (!epics.containsKey(subtask.getEpicId())) {
            return null;
        }
        subtask.setId(generateId());
        subtasks.put(subtask.getId(), subtask);
        tasks.put(subtask.getId(), subtask);

        Epic epic = epics.get(subtask.getEpicId());
        epic.addSubtask(subtask.getId());
        updateEpicStatus(epic);

        return subtask;
    }

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

    public boolean deleteSubtaskById(int id) {
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

    // === Дополнительно ===

    public List<Subtask> getSubtasksByEpicId(int epicId) {
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
        List<Subtask> subtasks = getSubtasksByEpicId(epic.getId());

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
}