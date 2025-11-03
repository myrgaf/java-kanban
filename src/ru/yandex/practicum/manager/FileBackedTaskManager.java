package ru.yandex.practicum.manager;

import ru.yandex.practicum.exceptions.ManagerSaveException;
import ru.yandex.practicum.models.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class FileBackedTaskManager extends InMemoryTaskManager {

    private final File file;

    public FileBackedTaskManager(File file) {
        this.file = file;
    }

    private void save() {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("id,type,title,status,description,epic,startTime,duration\n");

            for (Task task : getAllTasks()) {
                sb.append(taskToString(task)).append("\n");
            }
            for (Epic epic : getAllEpics()) {
                sb.append(taskToString(epic)).append("\n");
            }
            for (Subtask subtask : getAllSubtasks()) {
                sb.append(taskToString(subtask)).append("\n");
            }

            Files.writeString(file.toPath(), sb.toString());
        } catch (IOException e) {
            throw new ManagerSaveException(String.format("Не удалось сохранить данные в файл: %s", file.getAbsolutePath()));
        }
    }

    private String taskToString(Task task) {
        String startTimeStr = "";
        if (task.getStartTime() != null) {
            startTimeStr = task.getStartTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        }
        long durationMinutes = task.getDuration().toMinutes();

        if (task instanceof Subtask sub) {
            return String.format("%d,%s,%s,%s,%s,%d,%s,%d",
                    task.getId(),
                    TaskType.SUBTASK,
                    task.getTitle(),
                    task.getStatus(),
                    task.getDescription() != null ? task.getDescription() : "",
                    sub.getEpicId(),
                    startTimeStr,
                    durationMinutes
            );
        } else if (task instanceof Epic) {
            return String.format("%d,%s,%s,%s,%s,,%s,%d",
                    task.getId(),
                    TaskType.EPIC,
                    task.getTitle(),
                    task.getStatus(),
                    task.getDescription() != null ? task.getDescription() : "",
                    startTimeStr,
                    durationMinutes
            );
        } else {
            return String.format("%d,%s,%s,%s,%s,,%s,%d",
                    task.getId(),
                    TaskType.TASK,
                    task.getTitle(),
                    task.getStatus(),
                    task.getDescription() != null ? task.getDescription() : "",
                    startTimeStr,
                    durationMinutes
            );
        }
    }

    public static FileBackedTaskManager loadFromFile(File file) {
        FileBackedTaskManager manager = new FileBackedTaskManager(file);

        try {
            if (!file.exists() || file.length() == 0) {
                return manager;
            }

            String content = Files.readString(file.toPath());
            String[] lines = content.split("\n");
            if (lines.length <= 1) {
                return manager;
            }

            int maxId = 0;

            for (int i = 1; i < lines.length; i++) {
                String line = lines[i].trim();
                if (line.isEmpty()) continue;

                Task task = taskFromString(line);
                if (task.getId() > maxId) {
                    maxId = task.getId();
                }

                if (task instanceof Epic epic) {
                    manager.epics.put(epic.getId(), epic);
                    manager.tasks.put(epic.getId(), epic);
                } else if (task instanceof Subtask sub) {
                    manager.subtasks.put(sub.getId(), sub);
                    manager.tasks.put(sub.getId(), sub);
                } else {
                    manager.tasks.put(task.getId(), task);
                }
            }

            manager.nextId = maxId + 1;

            for (Subtask sub : manager.subtasks.values()) {
                Epic epic = manager.epics.get(sub.getEpicId());
                if (epic != null && !epic.getSubtaskIds().contains(sub.getId())) {
                    epic.addSubtask(sub.getId());
                }
            }

            for (Epic epic : manager.epics.values()) {
                manager.updateEpicStatus(epic);
            }

            for (Epic epic : manager.epics.values()) {
                manager.updateEpicTimes(epic);
            }

        } catch (IOException e) {
            throw new ManagerSaveException(String.format("Не удалось сохранить данные в файл: %s", file.getAbsolutePath()));
        }

        return manager;
    }

    private static Task taskFromString(String value) {
        String[] parts = value.split(",", -1);
        if (parts.length < 8) {
            throw new IllegalArgumentException("Неверный формат строки: " + value);
        }

        int id = Integer.parseInt(parts[0]);
        TaskType type = TaskType.valueOf(parts[1]);
        String title = parts[2];
        Status status = Status.valueOf(parts[3]);
        String description = parts[4].isEmpty() ? null : parts[4];

        String startTimeStr = parts[6];
        long durationMinutes = Long.parseLong(parts[7]);

        LocalDateTime startTime = null;
        if (!startTimeStr.isEmpty()) {
            startTime = LocalDateTime.parse(startTimeStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        }
        Duration duration = Duration.ofMinutes(durationMinutes);

        switch (type) {
            case TASK:
                Task task = new Task(title, description, status);
                task.setId(id);
                task.setStartTime(startTime);
                task.setDuration(duration);
                return task;
            case EPIC:
                Epic epic = new Epic(title, description, status);
                epic.setId(id);
                epic.setStartTime(startTime);
                epic.setDuration(duration);
                return epic;
            case SUBTASK:
                int epicId = parts[5].isEmpty() ? 0 : Integer.parseInt(parts[5]);
                Subtask subtask = new Subtask(title, description, status, epicId);
                subtask.setId(id);
                subtask.setStartTime(startTime);
                subtask.setDuration(duration);
                return subtask;
            default:
                throw new IllegalArgumentException(String.format("Неизвестный тип задачи: %s", type));
        }
    }

    @Override
    public Task createTask(Task task) {
        Task created = super.createTask(task);
        save();
        return created;
    }

    @Override
    public Epic createEpic(Epic epic) {
        Epic created = super.createEpic(epic);
        save();
        return created;
    }

    @Override
    public Subtask createSubtask(Subtask subtask) {
        Subtask created = super.createSubtask(subtask);
        if (created != null) {
            save();
        }
        return created;
    }

    @Override
    public Task updateTask(Task task) {
        Task updated = super.updateTask(task);
        if (updated != null) {
            save();
        }
        return updated;
    }

    @Override
    public Epic updateEpic(Epic epic) {
        Epic updated = super.updateEpic(epic);
        if (updated != null) {
            save();
        }
        return updated;
    }

    @Override
    public Subtask updateSubtask(Subtask subtask) {
        Subtask updated = super.updateSubtask(subtask);
        if (updated != null) {
            save();
        }
        return updated;
    }

    @Override
    public boolean deleteTask(int id) {
        boolean deleted = super.deleteTask(id);
        if (deleted) {
            save();
        }
        return deleted;
    }

    @Override
    public boolean deleteEpic(int id) {
        boolean deleted = super.deleteEpic(id);
        if (deleted) {
            save();
        }
        return deleted;
    }

    @Override
    public boolean deleteSubtask(int id) {
        boolean deleted = super.deleteSubtask(id);
        if (deleted) {
            save();
        }
        return deleted;
    }
}