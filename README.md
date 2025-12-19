# NewsHub

**NewsHub** — веб-приложение для парсинга RSS-новостей и сохранения их в базу данных, использующее Docker.
Приложение загружает новости из выбранных источников, сохраняет их в **PostgreSQL** и отображает через веб-интерфейс.

---

## Архитектура

Проект состоит минимум из **двух сервисов**:

* **newshub** — веб-приложение (UI + API), хранит новости в PostgreSQL, отдаёт страницы/эндпоинты, читает события из Kafka
* **newshub-ingestor** — сервис загрузки RSS: по команде обновления выполняет парсинг и отправляет новости в Kafka.

---

## Технологии

- Java 17+
- Spring Boot
- MySQL
- Docker
- Apache Kafka

---

## Как запустить проект

### 1. Запуск инфраструктуры (PostgreSQL + Kafka)

Убедитесь, что **Docker Desktop запущен**.

В корне проекта выполните:

```bash
docker compose pull
```

---

### 2. Запуск приложения

В корне проекта выполните:

```bash
docker compose up -d
```

#### В IntelliJ IDEA (рекомендуется)

Выполните запуск обоих сервисов как Spring Boot.

---

#### Через терминал (Windows)

```powershell
.\mvnw -pl newshub spring-boot:run
```

```powershell
.\mvnw -pl newshub-ingestor spring-boot:run
```

---

## Проверка работы

Откройте в браузере:

```
http://localhost:8080
```

