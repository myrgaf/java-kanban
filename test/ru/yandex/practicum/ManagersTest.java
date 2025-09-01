package ru.yandex.practicum;

import org.junit.jupiter.api.Test;
import ru.yandex.practicum.manager.Managers;
import ru.yandex.practicum.manager.TaskManager;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class ManagersTest {
    @Test
    void returnNonNull() {
        TaskManager manager = Managers.getDefault();
        assertNotNull(manager);
    }
}