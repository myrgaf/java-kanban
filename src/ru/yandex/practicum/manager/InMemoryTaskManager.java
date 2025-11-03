package ru.yandex.practicum.manager;

import ru.yandex.practicum.exceptions.TaskIntersectionException;
import ru.yandex.practicum.models.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class InMemoryTaskManager implements TaskManager {
    protected final Map<Integer, Task> tasks;
    protected final Map<Integer, Epic> epics;
    protected final Map<Integer, Subtask> subtasks;
    protected int nextId;
    private final HistoryManager historyManager;
    private final TreeSet<Task> prioritizedTasks = new TreeSet<>(
            Comparator
                    .comparing(Task::getStartTime, Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(Task::getId)
    );

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
        return tasks.values().stream()
                .filter(task -> !(task instanceof Subtask) && !(task instanceof Epic))
                .collect(Collectors.toList());
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
        if (hasIntersectionsWith(task)) {
            throw new TaskIntersectionException(
                    "Невозможно создать задачу: она пересекается по времени с другой задачей."
            );
        }

        Task copy = new Task(task.getTitle(), task.getDescription(), task.getStatus());
        if (task.getId() == 0) {
            copy.setId(generateId());
        } else {
            copy.setId(task.getId());
        }
        copy.setStartTime(task.getStartTime());
        copy.setDuration(task.getDuration());

        tasks.put(copy.getId(), copy);
        if (copy.getStartTime() != null) {
            prioritizedTasks.add(copy);
        }
        return copy;
    }

    @Override
    public Task updateTask(Task updatedTask) {
        if (hasIntersectionsWith(updatedTask)) {
            throw new TaskIntersectionException("Невозможно обновить задачу: она пересекается по времени.");
        }

        if (tasks.containsKey(updatedTask.getId())) {
            Task existing = tasks.get(updatedTask.getId());
            if (existing != null && !(existing instanceof Epic || existing instanceof Subtask)) {
                if (existing.getStartTime() != null) {
                    prioritizedTasks.remove(existing);
                }

                existing.setTitle(updatedTask.getTitle());
                existing.setDescription(updatedTask.getDescription());
                existing.setStatus(updatedTask.getStatus());
                existing.setStartTime(updatedTask.getStartTime());
                existing.setDuration(updatedTask.getDuration());

                if (existing.getStartTime() != null) {
                    prioritizedTasks.add(existing);
                }

                return existing;
            }
        }
        return null;
    }

    @Override
    public boolean deleteTask(int id) {
        Task task = tasks.get(id);
        if (task != null && !(task instanceof Epic || task instanceof Subtask)) {
            if (task.getStartTime() != null) {
                prioritizedTasks.remove(task);
            }
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

    void updateEpicStatus(Epic epic) {
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

    protected void updateEpicTimes(Epic epic) {
        if (epic == null) return;

        List<Subtask> epicSubtasks = epic.getSubtaskIds().stream()
                .map(subtasks::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (epicSubtasks.isEmpty()) {
            epic.setStartTime(null);
            epic.setDuration(Duration.ZERO);
            epic.setEndTime(null);
            return;
        }

        LocalDateTime minStart = epicSubtasks.stream()
                .map(Subtask::getStartTime)
                .filter(Objects::nonNull)
                .min(LocalDateTime::compareTo)
                .orElse(null);

        LocalDateTime maxEnd = epicSubtasks.stream()
                .map(Subtask::getEndTime)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null);

        Duration totalDuration = epicSubtasks.stream()
                .map(Subtask::getDuration)
                .reduce(Duration.ZERO, Duration::plus);

        epic.setStartTime(minStart);
        epic.setDuration(totalDuration);
        epic.setEndTime(maxEnd);
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
            updateEpicTimes(epic);
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

        if (hasIntersectionsWith(subtask)) {
            throw new TaskIntersectionException(
                    "Невозможно создать подзадачу: она пересекается по времени с другой задачей."
            );
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
        copy.setStartTime(subtask.getStartTime());
        copy.setDuration(subtask.getDuration());

        subtasks.put(copy.getId(), copy);
        tasks.put(copy.getId(), copy);

        Epic epic = epics.get(copy.getEpicId());
        epic.addSubtask(copy.getId());
        updateEpicStatus(epic);
        updateEpicTimes(epic);

        if (copy.getStartTime() != null) {
            prioritizedTasks.add(copy);
        }

        return copy;
    }

    @Override
    public Subtask updateSubtask(Subtask updatedSubtask) {
        Subtask subtask = subtasks.get(updatedSubtask.getId());
        if (subtask == null) return null;

        if (hasIntersectionsWith(updatedSubtask)) {
            throw new TaskIntersectionException("Невозможно обновить задачу: она пересекается по времени.");
        }

        if (subtask.getStartTime() != null) {
            prioritizedTasks.remove(subtask);
        }

        subtask.setTitle(updatedSubtask.getTitle());
        subtask.setDescription(updatedSubtask.getDescription());
        subtask.setStatus(updatedSubtask.getStatus());
        subtask.setStartTime(updatedSubtask.getStartTime());
        subtask.setDuration(updatedSubtask.getDuration());

        int oldEpicId = subtask.getEpicId();
        int newEpicId = updatedSubtask.getEpicId();

        if (oldEpicId != newEpicId) {
            Epic oldEpic = epics.get(oldEpicId);
            if (oldEpic != null) {
                oldEpic.removeSubtask(subtask.getId());
                updateEpicStatus(oldEpic);
                updateEpicTimes(oldEpic);
            }

            Epic newEpic = epics.get(newEpicId);
            if (newEpic != null) {
                newEpic.addSubtask(subtask.getId());
                updateEpicStatus(newEpic);
                updateEpicTimes(newEpic);
            }
            subtask.setEpicId(newEpicId);
        } else {
            Epic epic = epics.get(oldEpicId);
            if (epic != null) {
                updateEpicStatus(epic);
                updateEpicTimes(epic);
            }
        }

        if (subtask.getStartTime() != null) {
            prioritizedTasks.add(subtask);
        }

        return subtask;
    }

    @Override
    public boolean deleteSubtask(int id) {
        Subtask subtask = subtasks.remove(id);
        if (subtask != null) {
            tasks.remove(id);

            if (subtask.getStartTime() != null) {
                prioritizedTasks.remove(subtask);
            }

            Epic epic = epics.get(subtask.getEpicId());
            if (epic != null) {
                epic.removeSubtask(id);
                updateEpicStatus(epic);
                updateEpicTimes(epic);
            }
            return true;
        }
        return false;
    }


    @Override
    public List<Subtask> getEpicSubtasks(int epicId) {
        Epic epic = epics.get(epicId);
        if (epic == null) return List.of();

        return epic.getSubtaskIds().stream()
                .map(subtasks::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }


    @Override
    public List<Task> getHistory() {
        return historyManager.getHistory();
    }

    private boolean isIntersect(Task a, Task b) {
        if (a == null || b == null) return false;
        if (a.getStartTime() == null || b.getStartTime() == null) return false;

        LocalDateTime aEnd = a.getEndTime();
        LocalDateTime bEnd = b.getEndTime();

        if (aEnd == null || bEnd == null) return false;

        return a.getStartTime().isBefore(bEnd) && b.getStartTime().isBefore(aEnd);
    }

    private boolean hasIntersectionsWith(Task task) {
        if (task == null || task.getStartTime() == null) {
            return false;
        }

        return getAllExecutableTasks().stream()
                .filter(t -> !t.equals(task))
                .anyMatch(existing -> isIntersect(task, existing));
    }

    private List<Task> getAllExecutableTasks() {
        List<Task> all = new ArrayList<>();
        all.addAll(getAllTasks());
        all.addAll(getAllSubtasks());
        return all;
    }

    @Override
    public List<Task> getPrioritizedTasks() {
        return new ArrayList<>(prioritizedTasks);
    }
}