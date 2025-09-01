package ru.yandex.practicum.models;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TaskTest {
    @Test
    void tasksWithSameIdShouldBeEqual() {
        Task task1 = new Task("Задача 1","Описание", Status.NEW);
        task1.setId(1);
        Task task2 = new Task("Задача 2", "Описание", Status.NEW);
        task2.setId(1);

        assertEquals(task1, task2);
        assertEquals(task1.hashCode(), task2.hashCode());
    }

    @Test
    void subtaskAndEpicWithSameIdShouldNotBeEqual() {
        Epic epic = new Epic("Эпик 1", "Описание", Status.NEW);
        epic.setId(1);
        Subtask subtask = new Subtask("Подзадача 1", "Описание", Status.NEW, 2);
        subtask.setId(1);

        assertNotEquals(epic, subtask);
    }

    @Test
    void epicsWithSameIdShouldBeEqual() {
        Epic epic1 = new Epic("Эпик 1", "Описание", Status.NEW);
        epic1.setId(1);
        Epic epic2 = new Epic("Эпик 2", "Описание", Status.NEW);
        epic2.setId(1);

        assertEquals(epic1, epic2);
        assertEquals(epic1.hashCode(), epic2.hashCode());
    }

    @Test
    void subtasksWithSameIdShouldBeEqual() {
        Subtask s1 = new Subtask("Подзадача 1", "Описание", Status.NEW, 1);
        s1.setId(1);
        Subtask s2 = new Subtask("Подзадача 2", "Описание", Status.NEW, 2);
        s2.setId(1);

        assertEquals(s1, s2);
    }
}