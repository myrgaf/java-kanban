package ru.yandex.practicum;

import ru.yandex.practicum.manager.FileBackedTaskManager;
import ru.yandex.practicum.manager.Managers;
import ru.yandex.practicum.manager.TaskManager;
import ru.yandex.practicum.models.*;

import java.io.File;
import java.io.IOException;


public class Main {
    private static void printAllTasks(TaskManager manager) {
        System.out.println("Задачи:");
        for (Task task : manager.getAllTasks()) {
            System.out.println(task);
        }
        System.out.println("Эпики:");
        for (Epic epic : manager.getAllEpics()) {
            System.out.println(epic);

            for (Task task : manager.getEpicSubtasks(epic.getId())) {
                System.out.println(" - " + task);
            }
        }
        System.out.println("Подзадачи:");
        for (Task subtask : manager.getAllSubtasks()) {
            System.out.println(subtask);
        }

        System.out.println("История:");
        for (Task task : manager.getHistory()) {
            System.out.println(task);
        }
        System.out.println(" — ".repeat(40));
    }

    public static void main(String[] args) throws IOException {
        TaskManager manager = Managers.getDefault();

        // Обычные задачи
        Task task1 = manager.createTask(new Task("Покупка", "Купить молоко", Status.NEW));
        Task task2 = manager.createTask(new Task("Уборка", "Протереть пыль", Status.IN_PROGRESS));

        // Эпики и подзадачи
        Epic epic1 = manager.createEpic(new Epic("Переезд", "Собрать вещи", Status.NEW));
        Subtask sub1 = manager.createSubtask(new Subtask("Собрать вещи", "Книги", Status.NEW, epic1.getId()));
        Subtask sub2 = manager.createSubtask(new Subtask("Вызвать грузчиков", "Заказать машину", Status.IN_PROGRESS, epic1.getId()));

        Epic epic2 = manager.createEpic(new Epic("Ремонт", "Покраска стен", Status.NEW));
        Subtask sub3 = manager.createSubtask(new Subtask("Покрасить", "Гостиную", Status.DONE, epic2.getId()));

        printAllTasks(manager);

        // Проверка истории
        manager.getTask(task1.getId());
        manager.getEpic(epic1.getId());
        manager.getSubtask(sub2.getId());
        manager.getTask(task1.getId()); // дубль
        manager.getEpic(epic2.getId());

        printAllTasks(manager);


        // Проверка функционала сохранения в файл.csv и восстановления из файла.csv

        File tempFile = File.createTempFile("test", ".csv");
        System.out.println("Файл: " + tempFile.getAbsolutePath());

        FileBackedTaskManager backendManager = new FileBackedTaskManager(tempFile);

        Task task = backendManager.createTask(new Task("Тест", "Описание", Status.NEW));
        Epic epic = backendManager.createEpic(new Epic("Эпик", "Описание", Status.NEW));
        Subtask sub = backendManager.createSubtask(new Subtask("Подзадача", "Описание", Status.DONE, epic.getId()));

        System.out.println("Сохранено. Перезагружаем...");

        FileBackedTaskManager loaded = FileBackedTaskManager.loadFromFile(tempFile);

        System.out.println("Задачи: " + loaded.getAllTasks().size());
        System.out.println("Эпики: " + loaded.getAllEpics().size());
        System.out.println("Подзадачи: " + loaded.getAllSubtasks().size());
        System.out.println("Статус эпика: " + loaded.getEpic(epic.getId()).getStatus());
    }
}