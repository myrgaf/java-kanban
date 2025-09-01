package ru.yandex.practicum.manager;;

public class Managers {
    // Класс отвечает за создание менеджера задач
    public static TaskManager getDefault() {
        return new InMemoryTaskManager(getDefaultHistory());
    }

    public static HistoryManager getDefaultHistory() {
        return new InMemoryHistoryManager();
    }
}