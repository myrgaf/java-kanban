package ru.yandex.practicum.manager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.yandex.practicum.exceptions.TaskIntersectionException;
import ru.yandex.practicum.models.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public abstract class AbstractTaskManagerTest<T extends TaskManager> {

    protected T manager;

    @BeforeEach
    void setUp() {
        manager = createManager();
    }

    protected abstract T createManager();

    // === Epic status ===

    @Test
    void epicStatusAllNew() {
        Epic epic = new Epic("Epic", "Desc", Status.NEW);
        manager.createEpic(epic);

        Subtask s1 = new Subtask("S1", "D", Status.NEW, epic.getId());
        Subtask s2 = new Subtask("S2", "D", Status.NEW, epic.getId());
        manager.createSubtask(s1);
        manager.createSubtask(s2);

        assertEquals(Status.NEW, manager.getEpic(epic.getId()).getStatus());
    }

    @Test
    void epicStatusAllDone() {
        Epic epic = new Epic("Epic", "Desc", Status.NEW);
        manager.createEpic(epic);

        Subtask s1 = new Subtask("S1", "D", Status.DONE, epic.getId());
        Subtask s2 = new Subtask("S2", "D", Status.DONE, epic.getId());
        manager.createSubtask(s1);
        manager.createSubtask(s2);

        assertEquals(Status.DONE, manager.getEpic(epic.getId()).getStatus());
    }

    @Test
    void epicStatusInProgress() {
        Epic epic = new Epic("Epic", "Desc", Status.NEW);
        manager.createEpic(epic);

        Subtask s1 = new Subtask("S1", "D", Status.NEW, epic.getId());
        Subtask s2 = new Subtask("S2", "D", Status.DONE, epic.getId());
        manager.createSubtask(s1);
        manager.createSubtask(s2);

        assertEquals(Status.IN_PROGRESS, manager.getEpic(epic.getId()).getStatus());
    }

    // === Epic times ===

    @Test
    void epicTimesCalculatedCorrectly() {
        Epic epic = new Epic("Epic", "Desc", Status.NEW);
        manager.createEpic(epic);

        Subtask s1 = new Subtask("S1", "D", Status.NEW, epic.getId());
        s1.setStartTime(LocalDateTime.of(2025, 11, 3, 9, 0));
        s1.setDuration(Duration.ofMinutes(90)); // ends at 10:30

        Subtask s2 = new Subtask("S2", "D", Status.NEW, epic.getId());
        s2.setStartTime(LocalDateTime.of(2025, 11, 3, 10, 0));
        s2.setDuration(Duration.ofMinutes(60)); // ends at 11:00

        manager.createSubtask(s1);
        manager.createSubtask(s2);

        Epic e = manager.getEpic(epic.getId());
        assertEquals(LocalDateTime.of(2025, 11, 3, 9, 0), e.getStartTime());
        assertEquals(LocalDateTime.of(2025, 11, 3, 11, 0), e.getEndTime());
        assertEquals(Duration.ofMinutes(150), e.getDuration());
    }

    // === Prioritized tasks ===

    @Test
    void prioritizedTasksSortedByStartTime() {
        Task t1 = new Task("T1", "D", Status.NEW);
        t1.setStartTime(LocalDateTime.of(2025, 11, 3, 11, 0));
        t1.setDuration(Duration.ofMinutes(30));

        Epic epic = new Epic("Epic", "D", Status.NEW);
        manager.createEpic(epic);

        Subtask s1 = new Subtask("S1", "D", Status.NEW, epic.getId());
        s1.setStartTime(LocalDateTime.of(2025, 11, 3, 10, 0));
        s1.setDuration(Duration.ofMinutes(45));

        Task savedT1 = manager.createTask(t1);
        Subtask savedS1 = manager.createSubtask(s1);

        List<Task> list = manager.getPrioritizedTasks();
        assertEquals(2, list.size());
        assertEquals(savedS1, list.get(0));
        assertEquals(savedT1, list.get(1));
    }

    // === Intersection tests ===

    @Test
    void taskIntersectsWithSubtask() {
        Epic epic = new Epic("E", "D", Status.NEW);
        manager.createEpic(epic);

        Subtask s1 = new Subtask("S", "D", Status.NEW, epic.getId());
        s1.setStartTime(LocalDateTime.of(2025, 11, 3, 10, 0));
        s1.setDuration(Duration.ofMinutes(60));
        manager.createSubtask(s1);

        Task t1 = new Task("T", "D", Status.NEW);
        t1.setStartTime(LocalDateTime.of(2025, 11, 3, 10, 30));
        t1.setDuration(Duration.ofMinutes(30));

        assertThrows(TaskIntersectionException.class, () -> manager.createTask(t1));
    }

    @Test
    void nonIntersectingTasksAllowed() {
        Task t1 = new Task("T1", "D", Status.NEW);
        t1.setStartTime(LocalDateTime.of(2025, 11, 3, 9, 0));
        t1.setDuration(Duration.ofMinutes(60)); // ends at 10:00

        Task t2 = new Task("T2", "D", Status.NEW);
        t2.setStartTime(LocalDateTime.of(2025, 11, 3, 10, 0)); // starts exactly at end
        t2.setDuration(Duration.ofMinutes(30));

        manager.createTask(t1);
        manager.createTask(t2); // should not throw

        assertEquals(2, manager.getPrioritizedTasks().size());
    }
}