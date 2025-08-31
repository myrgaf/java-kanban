package ru.yandex.practicum.models;

// src/ru.yandex.practicum.models.Subtask.java
public class Subtask extends Task {
    private int epicId;

    public Subtask(String title, String description, Status status, int epicId) {
        super(title, description, status);
        this.epicId = epicId;
    }

    public int getEpicId() {
        return epicId;
    }

    public void setEpicId(int epicId) {
        this.epicId = epicId;
    }

    @Override
    public String toString() {
        return String.format("Subtask{id=%d, title='%s', status=%s, epicId=%d}",
                getId(), getTitle(), getStatus(), epicId); // Как-то после рефакторинга сюда попало =\
    }
}