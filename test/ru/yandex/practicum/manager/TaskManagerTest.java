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

        Subtask s1 = new Subtask("S1", "D", Status.NEW, epic.getId());
        Subtask s2 = new Subtask("S2", "D", Status.NEW, epic.getId());
        manager.createSubtask(s1);
        manager.createSubtask(s2);

        assertEquals(Status.NEW, manager.getEpic(epic.getId()).getStatus());
    }

    @Test
    void shouldSetEpicStatusToDoneWhenAllSubtasksAreDone() {
        Epic epic = new Epic("Epic", "Desc", Status.NEW);
        manager.createEpic(epic);

        Subtask s1 = new Subtask("S1", "D", Status.DONE, epic.getId());
        Subtask s2 = new Subtask("S2", "D", Status.DONE, epic.getId());
        manager.createSubtask(s1);
        manager.createSubtask(s2);

        assertEquals(Status.DONE, manager.getEpic(epic.getId()).getStatus());
    }

    @Test
    void shouldSetEpicStatusToInProgressWhenMixed() {
        Epic epic = new Epic("Epic", "Desc", Status.NEW);
        manager.createEpic(epic);

        Subtask s1 = new Subtask("S1", "D", Status.NEW, epic.getId());
        Subtask s2 = new Subtask("S2", "D", Status.DONE, epic.getId());
        manager.createSubtask(s1);
        manager.createSubtask(s2);

        assertEquals(Status.IN_PROGRESS, manager.getEpic(epic.getId()).getStatus());
    }

    @Test
    void shouldSetEpicStatusToInProgressWhenOneIsInProgress() {
        Epic epic = new Epic("Epic", "Desc", Status.NEW);
        manager.createEpic(epic);

        Subtask s1 = new Subtask("S1", "D", Status.NEW, epic.getId());
        Subtask s2 = new Subtask("S2", "D", Status.IN_PROGRESS, epic.getId());
        manager.createSubtask(s1);
        manager.createSubtask(s2);

        assertEquals(Status.IN_PROGRESS, manager.getEpic(epic.getId()).getStatus());
    }

    @Test
    void shouldCalculateEpicTimesFromSubtasks() {
        Epic epic = new Epic("Epic", "Desc", Status.NEW);
        manager.createEpic(epic);

        Subtask s1 = new Subtask("S1", "D", Status.NEW, epic.getId());
        s1.setStartTime(LocalDateTime.of(2025, 11, 3, 9, 0));
        s1.setDuration(Duration.ofMinutes(90));

        Subtask s2 = new Subtask("S2", "D", Status.NEW, epic.getId());
        s2.setStartTime(LocalDateTime.of(2025, 11, 3, 10, 30));
        s2.setDuration(Duration.ofMinutes(30));

        manager.createSubtask(s1);
        manager.createSubtask(s2);

        Epic e = manager.getEpic(epic.getId());
        assertEquals(LocalDateTime.of(2025, 11, 3, 9, 0), e.getStartTime());
        assertEquals(LocalDateTime.of(2025, 11, 3, 11, 0), e.getEndTime());
        assertEquals(Duration.ofMinutes(120), e.getDuration());
    }

    @Test
    void shouldThrowExceptionWhenCreatingIntersectingTasks() {
        Task t1 = new Task("T1", "D", Status.NEW);
        t1.setStartTime(LocalDateTime.of(2025, 11, 3, 10, 0));
        t1.setDuration(Duration.ofMinutes(60));
        manager.createTask(t1);

        Task task2 = new Task("T2", "D", Status.NEW);
        task2.setStartTime(LocalDateTime.of(2025, 11, 3, 10, 30));
        task2.setDuration(Duration.ofMinutes(60));

        assertThrows(TaskIntersectionException.class, () -> manager.createTask(task2));
    }

    @Test
    void shouldThrowExceptionWhenTaskIntersectsWithSubtask() {
        Epic epic = new Epic("Epic", "Desc", Status.NEW);
        manager.createEpic(epic);

        Subtask sub = new Subtask("Sub", "D", Status.NEW, epic.getId());
        sub.setStartTime(LocalDateTime.of(2025, 11, 3, 10, 0));
        sub.setDuration(Duration.ofMinutes(60));
        manager.createSubtask(sub);

        Task task = new Task("Task", "D", Status.NEW);
        task.setStartTime(LocalDateTime.of(2025, 11, 3, 10, 30));
        task.setDuration(Duration.ofMinutes(60));

        assertThrows(TaskIntersectionException.class, () -> manager.createTask(task));
    }

    @Test
    void shouldReturnPrioritizedTasksSortedByStartTime() {
        Task t1 = new Task("T1", "D", Status.NEW);
        t1.setStartTime(LocalDateTime.of(2025, 11, 3, 11, 0));
        t1.setDuration(Duration.ofMinutes(30));

        Epic epic = new Epic("Epic", "Desc", Status.NEW);
        manager.createEpic(epic);

        Subtask s1 = new Subtask("S1", "D", Status.NEW, epic.getId());
        s1.setStartTime(LocalDateTime.of(2025, 11, 3, 10, 0));
        s1.setDuration(Duration.ofMinutes(30));

        Task savedT1 = manager.createTask(t1);
        Subtask savedS1 = manager.createSubtask(s1);

        List<Task> list = manager.getPrioritizedTasks();
        assertEquals(2, list.size());
        assertEquals(savedS1, list.get(0));
        assertEquals(savedT1, list.get(1));
    }

    @Test
    void shouldExcludeTasksWithoutStartTimeFromPrioritizedList() {
        Task t1 = new Task("T1", "D", Status.NEW);
        t1.setStartTime(LocalDateTime.of(2025, 11, 3, 10, 0));

        Task t2 = new Task("T2", "D", Status.NEW);
        // startTime = null

        manager.createTask(t1);
        manager.createTask(t2);

        List<Task> list = manager.getPrioritizedTasks();
        assertEquals(1, list.size());
        assertEquals("T1", list.get(0).getTitle());
    }
}