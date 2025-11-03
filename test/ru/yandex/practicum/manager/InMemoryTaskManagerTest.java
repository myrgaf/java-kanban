package ru.yandex.practicum.manager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.yandex.practicum.exceptions.TaskIntersectionException;
import ru.yandex.practicum.models.Epic;
import ru.yandex.practicum.models.Status;
import ru.yandex.practicum.models.Subtask;
import ru.yandex.practicum.models.Task;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryTaskManagerTest extends AbstractTaskManagerTest<InMemoryTaskManager> {
    private TaskManager manager;

    @BeforeEach
    void setUp() {
        manager = new InMemoryTaskManager();
    }

    @Override
    protected InMemoryTaskManager createManager() {
        return new InMemoryTaskManager();
    }

    @Test
    void checkEpicId() {
        Subtask subtask = new Subtask("Эпик 1", "Описание", Status.NEW, 1);
        subtask.setId(1); // id == epicId

        Subtask result = manager.createSubtask(subtask);
        assertNull(result);
    }

    @Test
    void addAndFindTaskById() {
        Task task = new Task("Задача 1", "Описание", Status.NEW);
        Task saved = manager.createTask(task);
        Task found = manager.getTask(saved.getId());
        assertNotNull(found);
        assertEquals(saved, found);
    }

    @Test
    void updateEpicStatus() {
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
    void duplicateInHistory() {
        Task task = manager.createTask(new Task("Задача 1", "Описание", Status.NEW));
        manager.getTask(task.getId());
        manager.getTask(task.getId()); // дубль

        List<Task> history = manager.getHistory();
        assertEquals(1, history.size());
        assertEquals(task, history.getFirst());
    }

    @Test
    void dublicateSubtask() {
        Epic epic = new Epic("Задача 1", "Описание", Status.NEW);
        epic = manager.createEpic(epic);

        Subtask selfSubtask = new Subtask("Эпик 1", "Описание", Status.NEW, epic.getId());
        selfSubtask.setId(epic.getId());

        Subtask result = manager.createSubtask(selfSubtask);
        assertNull(result, "Эпик 1 не должен быть своей подзадачей");
    }

    @Test
    void creatingTask() {
        Task original = new Task("Задача 1", "Описание", Status.NEW);
        original.setId(100);
        manager.createTask(original);
        original.setTitle("Задача 2");
        Task saved = manager.getTask(100);
        assertEquals("Задача 1", saved.getTitle());
        assertEquals("Задача 2", original.getTitle());
    }

    @Test
    void saveTaskState() {
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

    @Test
    void testGetPrioritizedTasksSortedByStartTime() {
        Task task1 = new Task("Задача 1", "Описание", Status.NEW);
        task1.setStartTime(LocalDateTime.of(2025, 11, 3, 11, 0));
        task1.setDuration(Duration.ofMinutes(30));
        Task savedTask = manager.createTask(task1);

        Epic epic = new Epic("Эпик", "Описание", Status.NEW);
        Epic savedEpic = manager.createEpic(epic);

        Subtask sub = new Subtask("Подзадача", "Описание", Status.NEW, savedEpic.getId());
        sub.setStartTime(LocalDateTime.of(2025, 11, 3, 10, 0));
        sub.setDuration(Duration.ofMinutes(45));
        Subtask savedSub = manager.createSubtask(sub);

        List<Task> prioritized = manager.getPrioritizedTasks();
        assertEquals(2, prioritized.size());
        assertEquals(savedSub, prioritized.get(0));
        assertEquals(savedTask, prioritized.get(1));
    }

    @Test
    void testTasksWithoutStartTimeExcludedFromPrioritizedList() {
        Task taskWithTime = new Task("Со временем", "Описание", Status.NEW);
        taskWithTime.setStartTime(LocalDateTime.of(2025, 11, 3, 10, 0));
        taskWithTime.setDuration(Duration.ofMinutes(30));

        Task taskWithoutTime = new Task("Без времени", "Описание", Status.NEW);

        manager.createTask(taskWithTime);
        manager.createTask(taskWithoutTime);

        List<Task> prioritized = manager.getPrioritizedTasks();
        assertEquals(1, prioritized.size());
        assertEquals("Со временем", prioritized.getFirst().getTitle());
    }

    @Test
    void testEpicTimesCalculatedFromSubtasks() {
        Epic epic = new Epic("Эпик", "Описание", Status.NEW);
        Epic savedEpic = manager.createEpic(epic);

        Subtask sub1 = new Subtask("Подзадача 1", "Описание", Status.NEW, savedEpic.getId());
        sub1.setStartTime(LocalDateTime.of(2025, 11, 3, 9, 0));
        sub1.setDuration(Duration.ofMinutes(60));

        Subtask sub2 = new Subtask("Подзадача 2", "Описание", Status.NEW, savedEpic.getId());
        sub2.setStartTime(LocalDateTime.of(2025, 11, 3, 10, 30));
        sub2.setDuration(Duration.ofMinutes(30));

        manager.createSubtask(sub1);
        manager.createSubtask(sub2);

        Epic updatedEpic = manager.getEpic(savedEpic.getId());
        assertEquals(LocalDateTime.of(2025, 11, 3, 9, 0), updatedEpic.getStartTime());
        assertEquals(Duration.ofMinutes(90), updatedEpic.getDuration());
        assertEquals(LocalDateTime.of(2025, 11, 3, 11, 0), updatedEpic.getEndTime());
    }

    @Test
    void testEpicWithNoSubtasksHasNullStartTimeAndZeroDuration() {
        Epic epic = new Epic("Пустой эпик", "Описание", Status.NEW);
        Epic savedEpic = manager.createEpic(epic);

        Epic loaded = manager.getEpic(savedEpic.getId());
        assertNull(loaded.getStartTime());
        assertEquals(Duration.ZERO, loaded.getDuration());
        assertNull(loaded.getEndTime());
    }

    @Test
    void testTaskIntersectionPreventsCreation() {
        Task task1 = new Task("Задача 1", "Описание", Status.NEW);
        task1.setStartTime(LocalDateTime.of(2025, 11, 3, 10, 0));
        task1.setDuration(Duration.ofMinutes(60));
        manager.createTask(task1);

        Task task2 = new Task("Задача 2", "Описание", Status.NEW);
        task2.setStartTime(LocalDateTime.of(2025, 11, 3, 10, 30));
        task2.setDuration(Duration.ofMinutes(60));

        assertThrows(TaskIntersectionException.class, () -> {
            manager.createTask(task2);
        });
    }

    @Test
    void testSubtaskIntersectionWithTaskPreventsCreation() {
        Task task = new Task("Задача", "Описание", Status.NEW);
        task.setStartTime(LocalDateTime.of(2025, 11, 3, 10, 0));
        task.setDuration(Duration.ofMinutes(60));
        manager.createTask(task);

        Epic epic = new Epic("Эпик", "Описание", Status.NEW);
        Epic savedEpic = manager.createEpic(epic);

        Subtask sub = new Subtask("Подзадача", "Описание", Status.NEW, savedEpic.getId());
        sub.setStartTime(LocalDateTime.of(2025, 11, 3, 10, 30));
        sub.setDuration(Duration.ofMinutes(60));

        assertThrows(TaskIntersectionException.class, () -> {
            manager.createSubtask(sub);
        });
    }
}