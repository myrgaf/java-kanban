package ru.yandex.practicum;

import ru.yandex.practicum.manager.Managers;
import ru.yandex.practicum.manager.TaskManager;
import ru.yandex.practicum.models.*;


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

    public static void main(String[] args) {
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
    }
}