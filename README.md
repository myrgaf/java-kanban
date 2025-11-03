# Трекер задач

Программа умеет:
- Хранить задачи, подзадачи и эпики.
- Управлять их статусами.
- Позволяет создавать, обновлять, удалять и получать задачи.
- Автоматически обновляет статус эпика на основе подзадач.
- Сохранять и восстанавливать состояние задач из .csv

Классы:
- [ ] ru.yandex.practicum.models.Task: Обычная задача
- [x] ru.yandex.practicum.models.Epic: Большая задача (состоит из подзадач)
- [x] ru.yandex.practicum.models.Subtask: Подзадача (связана с большой задачей)
- [x] ru.yandex.practicum.models.Status: Перечисление статусов задач
- [x] ru.yandex.practicum.manager.TaskManager: Менеджер задач
- [x] ru.yandex.practicum.manager.FileBackendTaskManager: Менеджер задач для сохаранения/ восстановление из .csv формата
- [x] ru.yandex.practicum.Main: Демонстрация работы