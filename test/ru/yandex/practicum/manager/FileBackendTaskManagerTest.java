package ru.yandex.practicum.manager;

import org.junit.jupiter.api.Test;
import ru.yandex.practicum.models.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

class FileBackedTaskManagerTest {

    @Test
    void testLoadFromEmptyFile() throws IOException {
        // Создается пустой файл .csv
        File tempFile = File.createTempFile("test", ".csv");
        FileBackedTaskManager loaded = FileBackedTaskManager.loadFromFile(tempFile);

        assertTrue(loaded.getAllTasks().isEmpty());
        assertTrue(loaded.getAllEpics().isEmpty());
        assertTrue(loaded.getAllSubtasks().isEmpty());

        Files.delete(tempFile.toPath());
    }

    @Test
    void testSaveAndLoadSingleTask() throws IOException {
        // Файл с одной записью Task
        File tempFile = File.createTempFile("test", ".csv");
        FileBackedTaskManager manager = new FileBackedTaskManager(tempFile);

        Task task = new Task("Сделать задачу", "Дополнительное описание", Status.DONE);
        manager.createTask(task);

        FileBackedTaskManager loaded = FileBackedTaskManager.loadFromFile(tempFile);

        assertEquals(1, loaded.getAllTasks().size());
        Task loadedTask = (Task) loaded.getAllTasks().getFirst();
        assertEquals("Сделать задачу", loadedTask.getTitle());
        assertEquals(Status.DONE, loadedTask.getStatus());
        assertEquals("Дополнительное описание", loadedTask.getDescription());

        Files.delete(tempFile.toPath());
    }

    @Test
    void testSaveAndLoadEpicWithSubtasks() throws IOException {
        // Файл с Epic и Subtasks
        File tempFile = File.createTempFile("test", ".csv");
        FileBackedTaskManager manager = new FileBackedTaskManager(tempFile);

        Epic epic = new Epic("Эпик 1", "Описание эпика 1", Status.NEW);
        Epic savedEpic = manager.createEpic(epic);

        Subtask sub1 = new Subtask("Подзадача 1", "Описание подзадачи 1", Status.DONE, savedEpic.getId());
        Subtask sub2 = new Subtask("Подзадача 2", "Описание подзадачи 2", Status.NEW, savedEpic.getId());

        manager.createSubtask(sub1);
        manager.createSubtask(sub2);

        assertEquals(Status.IN_PROGRESS, manager.getEpic(savedEpic.getId()).getStatus());

        FileBackedTaskManager loaded = FileBackedTaskManager.loadFromFile(tempFile);

        assertEquals(1, loaded.getAllEpics().size());
        assertEquals(2, loaded.getAllSubtasks().size());

        Epic loadedEpic = loaded.getEpic(savedEpic.getId());
        assertEquals(Status.IN_PROGRESS, loadedEpic.getStatus());

        assertEquals(2, loaded.getEpicSubtasks(loadedEpic.getId()).size());

        Files.delete(tempFile.toPath());
    }
}