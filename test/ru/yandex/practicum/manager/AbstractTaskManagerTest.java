package ru.yandex.practicum.manager;

import org.junit.jupiter.api.BeforeEach;

public abstract class AbstractTaskManagerTest<T extends TaskManager> {
    protected T manager;

    @BeforeEach
    void setUp() {
        manager = createManager();
    }

    protected abstract T createManager();

}