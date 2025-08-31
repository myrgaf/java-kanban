package ru.yandex.practicum;

import ru.yandex.practicum.models.Epic;
import ru.yandex.practicum.models.Status;
import ru.yandex.practicum.models.Subtask;
import ru.yandex.practicum.models.Task;
import ru.yandex.practicum.manager.TaskManager;


public class Main {
    public static void main(String[] args) {
        TaskManager manager = new TaskManager();

        // Обычные задачи
        Task task1 = manager.createTask(new Task("Покупка", "Купить молоко", Status.NEW));
        Task task2 = manager.createTask(new Task("Уборка", "Протереть пыль", Status.IN_PROGRESS));

        // Эпики и подзадачи
        Epic epic1 = manager.createEpic(new Epic("Переезд", "Собрать вещи", Status.NEW));
        Subtask sub1 = manager.createSubtask(new Subtask("Собрать вещи", "Книги", Status.NEW, epic1.getId()));
        Subtask sub2 = manager.createSubtask(new Subtask("Вызвать грузчиков", "Заказать машину", Status.IN_PROGRESS, epic1.getId()));

        Epic epic2 = manager.createEpic(new Epic("Ремонт", "Покраска стен", Status.NEW));
        Subtask sub3 = manager.createSubtask(new Subtask("Покрасить", "Гостиную", Status.DONE, epic2.getId()));

        System.out.println("=== Все обычные задачи ===");
        manager.getAllTasks().forEach(System.out::println);

        System.out.println("\n=== Все эпики ===");
        manager.getAllEpics().forEach(System.out::println);

        System.out.println("\n=== Подзадачи эпика 1 ===");
        manager.getSubtasksByEpicId(epic1.getId()).forEach(System.out::println);

        // Меняем статусы
        sub1.setStatus(Status.DONE);
        manager.updateSubtask(sub1);
        System.out.println("\nПосле завершения sub1: " + manager.getEpicById(epic1.getId()));

        sub2.setStatus(Status.DONE);
        manager.updateSubtask(sub2);
        System.out.println("После всех подзадач: " + manager.getEpicById(epic1.getId()));

        // Удаляем
        manager.deleteTaskById(task2.getId());
        manager.deleteEpicById(epic2.getId());

        System.out.println("\n=== После удалений ===");
        System.out.println("Обычных задач: " + manager.getAllTasks().size());
        System.out.println("Эпиков: " + manager.getAllEpics().size());
        System.out.println("Подзадач: " + manager.getAllSubtasks().size());
    }
}