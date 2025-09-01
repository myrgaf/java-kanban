package ru.yandex.practicum.manager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.yandex.practicum.models.Task;
import ru.yandex.practicum.models.Status;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryHistoryManagerTest {
    private HistoryManager historyManager;
    private Task task1, task2;

    @BeforeEach
    void setUp() {
        historyManager = new InMemoryHistoryManager();
        task1 = new Task("Задача 1", "Описание", Status.NEW);
        task1.setId(1);
        task2 = new Task("Задача 2", "Описание", Status.NEW);
        task2.setId(2);
    }

    @Test
    void addToHistory() {
        historyManager.add(task1);
        List<Task> history = historyManager.getHistory();
        assertEquals(1, history.size());
        assertEquals(task1, history.getFirst());
    }

    @Test
    void checkLimit() {
        for (int i = 0; i < 15; i++) {
            Task t = new Task("Задача № " + i, "Описание", Status.NEW);
            t.setId(i);
            historyManager.add(t);
        }
        assertEquals(10, historyManager.getHistory().size());
    }
}