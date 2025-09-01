package ru.yandex.practicum.manager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.yandex.practicum.models.Epic;
import ru.yandex.practicum.models.Status;
import ru.yandex.practicum.models.Subtask;
import ru.yandex.practicum.models.Task;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryTaskManagerTest {
    private TaskManager manager;

    @BeforeEach
    void setUp() {
        manager = new InMemoryTaskManager();
    }

    @Test
    void shouldNotAllowSubtaskToBeItsOwnEpic() {
        Subtask subtask = new Subtask("Эпик 1", "Описание", Status.NEW, 1);
        subtask.setId(1); // id == epicId

        Subtask result = manager.createSubtask(subtask);
        assertNull(result);
    }

    @Test
    void shouldAddAndFindTaskById() {
        Task task = new Task("Задача 1", "Описание", Status.NEW);
        Task saved = manager.createTask(task);
        Task found = manager.getTask(saved.getId());
        assertNotNull(found);
        assertEquals(saved, found);
    }

    @Test
    void shouldUpdateEpicStatusBasedOnSubtasks() {
        Epic epic = manager.createEpic(new Epic("Эпик 1", "Описание", Status.NEW));
        Subtask s1 = manager.createSubtask(new Subtask("Подзадача 1",
                "Описание", Status.NEW, epic.getId()));
        Subtask s2 = manager.createSubtask(new Subtask("Подзадача 2",
                "Описание", Status.IN_PROGRESS, epic.getId()));

        Epic founded = manager.getEpic(epic.getId());
        assertEquals(Status.IN_PROGRESS, founded.getStatus());

        s1.setStatus(Status.DONE);
        s2.setStatus(Status.DONE);
        manager.updateSubtask(s1);
        manager.updateSubtask(s2);

        founded = manager.getEpic(epic.getId());
        assertEquals(Status.DONE, founded.getStatus());
    }

    @Test
    void shouldAddToHistoryOnGet() {
        Task task = manager.createTask(new Task("Задача 1", "Описание", Status.NEW));
        manager.getTask(task.getId());
        manager.getTask(task.getId()); // дубль

        List<Task> history = manager.getHistory();
        assertEquals(1, history.size());
        assertEquals(task, history.getFirst());
    }

    @Test
    void shouldNotAllowEpicToBeItsOwnSubtask() {
        Epic epic = new Epic("Задача 1", "Описание", Status.NEW);
        epic = manager.createEpic(epic);

        Subtask selfSubtask = new Subtask("Эпик 1", "Описание", Status.NEW, epic.getId());
        selfSubtask.setId(epic.getId());

        Subtask result = manager.createSubtask(selfSubtask);
        assertNull(result, "Эпик 1 не должен быть своей подзадачей");
    }

    @Test
    void shouldNotModifyOriginalTaskOnCreation() {
        Task original = new Task("Задача 1", "Описание", Status.NEW);
        original.setId(100);
        manager.createTask(original);
        original.setTitle("Задача 2");
        Task saved = manager.getTask(100);
        assertEquals("Задача 1", saved.getTitle());
        assertEquals("Задача 2", original.getTitle());
    }

    @Test
    void historyShouldPreserveTaskStateAtTimeOfView() {
        Task task = new Task("Схожий текст", "Описание", Status.NEW);
        task.setId(1);
        manager.createTask(task);

        manager.getTask(1);

        task.setTitle("Измененный схожий текст");
        manager.updateTask(task);

        List<Task> history = manager.getHistory();
        assertEquals(1, history.size());
        assertEquals("Схожий текст", history.getFirst().getTitle());
    }

    @Test
    void deleteAllTasks() {
        Task task1 = new Task("Задача 1", "Описание", Status.NEW);
        Task task2 = new Task("Задача 2", "Описание", Status.NEW);
        manager.createTask(task1);
        manager.createTask(task2);

        List<Task> tasksBefore = manager.getAllTasks();
        assertEquals(2, tasksBefore.size(), "Должно быть 2 задачи до удаления");

        manager.deleteAllTasks();

        List<Task> tasksAfter = manager.getAllTasks();
        assertTrue(tasksAfter.isEmpty(), "Список задач должен быть пустым после deleteAllTasks()");
    }

    @Test
    void deleteTask() {
        Task task = new Task("Задача 1", "Описание", Status.NEW);
        Task created = manager.createTask(task);

        assertNotNull(created, "Задача должна быть создана");
        assertNotNull(manager.getTask(created.getId()), "Задача должна существовать до удаления");

        boolean isDeleted = manager.deleteTask(created.getId());

        assertTrue(isDeleted, "Вернул true при успешном удалении");
        assertNull(manager.getTask(created.getId()), "Задача должна быть удалена");
    }

    @Test
    void deleteSubtask() {
        Epic epic = manager.createEpic(new Epic("Эпик 1", "Описание", Status.NEW));

        // Создаём подзадачу
        Subtask subtask = new Subtask("Подзадача 1", "Описание", Status.IN_PROGRESS, epic.getId());
        Subtask created = manager.createSubtask(subtask);

        assertNotNull(created, "Подзадача должна быть создана");
        assertNotNull(manager.getSubtask(created.getId()), "Подзадача должна существовать до удаления");

        List<Subtask> epicSubtasks = manager.getEpicSubtasks(epic.getId());
        assertTrue(epicSubtasks.contains(created), "Подзадача должна быть в эпике");

        boolean isDeleted = manager.deleteSubtask(created.getId());

        assertTrue(isDeleted, "deleteSubtask() должен вернуть true");
        assertNull(manager.getSubtask(created.getId()), "Подзадача должна быть удалена");
        assertEquals(0, manager.getEpicSubtasks(epic.getId()).size(),
                "Подзадача должна быть удалена из эпика");
    }

    @Test
    void deleteEpic() {
        Epic epic = new Epic("Эпик 1", "Описание", Status.NEW);
        Epic createdEpic = manager.createEpic(epic);

        assertNotNull(createdEpic, "Эпик 1 должен быть создан");

        Subtask subtask1 = new Subtask("Подзадача 1", "Описание", Status.NEW, createdEpic.getId());
        Subtask subtask2 = new Subtask("Подзадача 2", "Описание", Status.IN_PROGRESS, createdEpic.getId());

        manager.createSubtask(subtask1);
        manager.createSubtask(subtask2);

        assertNotNull(manager.getEpic(createdEpic.getId()), "Эпик 1 должен существовать до удаления");
        assertEquals(2, manager.getEpicSubtasks(createdEpic.getId()).size(), "Должно быть 2 подзадачи");

        boolean isDeleted = manager.deleteEpic(createdEpic.getId());

        assertTrue(isDeleted, "Метод deleteEpic() должен вернуть true при успешном удалении");
        assertNull(manager.getEpic(createdEpic.getId()), "Эпик должен быть удалён");

        assertNull(manager.getSubtask(subtask1.getId()), "Первая подзадача должна быть удалена");
        assertNull(manager.getSubtask(subtask2.getId()), "Вторая подзадача должна быть удалена");

        assertTrue(manager.getAllSubtasks().isEmpty(), "Список подзадач должен быть пустым после удаления эпика");

        assertFalse(manager.getAllTasks().contains(createdEpic), "Эпик не должен быть в общем списке задач");
    }
}