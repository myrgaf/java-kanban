package ru.yandex.practicum.manager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.yandex.practicum.exceptions.TaskIntersectionException;
import ru.yandex.practicum.models.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public abstract class TaskManagerTest<T extends TaskManager> {

    protected T manager;

    @BeforeEach
    void setUp() {
        manager = createManager();
    }

    protected abstract T createManager();


    @Test
    void shouldSetEpicStatusToNewWhenAllSubtasksAreNew() {
        Epic epic = new Epic("Epic", "Desc", Status.NEW);
        manager.createEpic(epic);

        Subtask subtask1 = new Subtask("S1", "D", Status.NEW, epic.getId());
        Subtask subtask2 = new Subtask("S2", "D", Status.NEW, epic.getId());
        manager.createSubtask(subtask1);
        manager.createSubtask(subtask2);

        assertEquals(Status.NEW, manager.getEpic(epic.getId()).getStatus());
    }

    @Test
    void shouldSetEpicStatusToDoneWhenAllSubtasksAreDone() {
        Epic epic = new Epic("Epic", "Desc", Status.NEW);
        manager.createEpic(epic);

        Subtask subtask1 = new Subtask("S1", "D", Status.DONE, epic.getId());
        Subtask subtask2 = new Subtask("S2", "D", Status.DONE, epic.getId());
        manager.createSubtask(subtask1);
        manager.createSubtask(subtask2);

        assertEquals(Status.DONE, manager.getEpic(epic.getId()).getStatus());
    }

    @Test
    void shouldSetEpicStatusToInProgressWhenMixed() {
        Epic epic = new Epic("Epic", "Desc", Status.NEW);
        manager.createEpic(epic);

        Subtask subtask1 = new Subtask("S1", "D", Status.NEW, epic.getId());
        Subtask subtask2 = new Subtask("S2", "D", Status.DONE, epic.getId());
        manager.createSubtask(subtask1);
        manager.createSubtask(subtask2);

        assertEquals(Status.IN_PROGRESS, manager.getEpic(epic.getId()).getStatus());
    }

    @Test
    void shouldSetEpicStatusToInProgressWhenOneIsInProgress() {
        Epic epic = new Epic("Epic", "Desc", Status.NEW);
        manager.createEpic(epic);

        Subtask subtask1 = new Subtask("S1", "D", Status.NEW, epic.getId());
        Subtask subtask2 = new Subtask("S2", "D", Status.IN_PROGRESS, epic.getId());
        manager.createSubtask(subtask1);
        manager.createSubtask(subtask2);

        assertEquals(Status.IN_PROGRESS, manager.getEpic(epic.getId()).getStatus());
    }

    @Test
    void shouldCalculateEpicTimesFromSubtasks() {
        Epic epic = new Epic("Epic", "Desc", Status.NEW);
        manager.createEpic(epic);

        Subtask subtask1 = new Subtask("S1", "D", Status.NEW, epic.getId());
        subtask1.setStartTime(LocalDateTime.of(2025, 11, 3, 9, 0));
        subtask1.setDuration(Duration.ofMinutes(90));

        Subtask subtask2 = new Subtask("S2", "D", Status.NEW, epic.getId());
        subtask2.setStartTime(LocalDateTime.of(2025, 11, 3, 10, 30));
        subtask2.setDuration(Duration.ofMinutes(30));

        manager.createSubtask(subtask1);
        manager.createSubtask(subtask2);

        Epic epic1 = manager.getEpic(epic.getId());
        assertEquals(LocalDateTime.of(2025, 11, 3, 9, 0), epic.getStartTime());
        assertEquals(LocalDateTime.of(2025, 11, 3, 11, 0), epic.getEndTime());
        assertEquals(Duration.ofMinutes(120), epic1.getDuration());
    }

    @Test
    void shouldThrowExceptionWhenCreatingIntersectingTasks() {
        Task task1 = new Task("T1", "D", Status.NEW);
        task1.setStartTime(LocalDateTime.of(2025, 11, 3, 10, 0));
        task1.setDuration(Duration.ofMinutes(60));
        manager.createTask(task1);

        Task task2 = new Task("T2", "D", Status.NEW);
        task2.setStartTime(LocalDateTime.of(2025, 11, 3, 10, 30));
        task2.setDuration(Duration.ofMinutes(60));

        assertThrows(TaskIntersectionException.class, () -> manager.createTask(task2));
    }

    @Test
    void shouldThrowExceptionWhenTaskIntersectsWithSubtask() {
        Epic epic = new Epic("Epic", "Desc", Status.NEW);
        manager.createEpic(epic);

        Subtask subtask = new Subtask("Sub", "D", Status.NEW, epic.getId());
        subtask.setStartTime(LocalDateTime.of(2025, 11, 3, 10, 0));
        subtask.setDuration(Duration.ofMinutes(60));
        manager.createSubtask(subtask);

        Task task = new Task("Task", "D", Status.NEW);
        task.setStartTime(LocalDateTime.of(2025, 11, 3, 10, 30));
        task.setDuration(Duration.ofMinutes(60));

        assertThrows(TaskIntersectionException.class, () -> manager.createTask(task));
    }

    @Test
    void shouldReturnPrioritizedTasksSortedByStartTime() {
        Task task1 = new Task("T1", "D", Status.NEW);
        task1.setStartTime(LocalDateTime.of(2025, 11, 3, 11, 0));
        task1.setDuration(Duration.ofMinutes(30));

        Epic epic = new Epic("Epic", "Desc", Status.NEW);
        manager.createEpic(epic);

        Subtask subtask1 = new Subtask("S1", "D", Status.NEW, epic.getId());
        subtask1.setStartTime(LocalDateTime.of(2025, 11, 3, 10, 0));
        subtask1.setDuration(Duration.ofMinutes(30));

        Task savedT1 = manager.createTask(task1);
        Subtask savedS1 = manager.createSubtask(subtask1);

        List<Task> list = manager.getPrioritizedTasks();
        assertEquals(2, list.size());
        assertEquals(savedS1, list.get(0));
        assertEquals(savedT1, list.get(1));
    }

    @Test
    void shouldExcludeTasksWithoutStartTimeFromPrioritizedList() {
        Task task1 = new Task("T1", "D", Status.NEW);
        task1.setStartTime(LocalDateTime.of(2025, 11, 3, 10, 0));

        Task task2 = new Task("T2", "D", Status.NEW);

        manager.createTask(task1);
        manager.createTask(task2);

        List<Task> list = manager.getPrioritizedTasks();
        assertEquals(1, list.size());
        assertEquals("T1", list.getFirst().getTitle());
    }
}