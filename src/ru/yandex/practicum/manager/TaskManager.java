package ru.yandex.practicum.manager;

import java.util.List;
import ru.yandex.practicum.models.*;

public interface TaskManager {
    // Task
    List<Task> getAllTasks();

    void deleteAllTasks();

    Task getTask(int id);

    Task createTask(Task task);

    Task updateTask(Task task);

    boolean deleteTask(int id);

    // Epic
    List<Epic> getAllEpics();

    void deleteAllEpics();

    List<Subtask> getEpicSubtasks(int epicId);

    Epic getEpic(int id);

    Epic createEpic(Epic epic);

    Epic updateEpic(Epic epic);

    boolean deleteEpic(int id);

    // Subtask
    List<Subtask> getAllSubtasks();
    void deleteAllSubtasks();

    Subtask getSubtask(int id);
    Subtask createSubtask(Subtask subtask);
    Subtask updateSubtask(Subtask subtask);
    boolean deleteSubtask(int id);

    // History
    List<Task> getHistory();
}