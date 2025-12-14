# NewsHub

**NewsHub** — веб-приложение для парсинга RSS-новостей и сохранения их в базу данных, использующее Docker.
Приложение загружает новости из выбранных источников, сохраняет их в **MySQL** и отображает через веб-интерфейс.

---

## Технологии

- Java 17+
- Spring Boot
- MySQL
- Docker

---

## Как запустить проект

### 1. Запуск базы данных (MySQL)

Убедитесь, что **Docker Desktop запущен**.
В корне проекта выполните:

```bash
docker compose up -d
```

---

### 2. Запуск приложения

#### В IntelliJ IDEA (рекомендуется)

1. Откройте проект
2. Run → Edit Configurations → Spring Boot
3. Main class:
   ```
   org.example.newshub.NewsHubApplication
   ```
4. Active profiles:
   ```
   mysql
   ```
5. JDK: **17 или 21**
6. Нажмите **Run**

---

#### Через терминал (Windows)

```powershell
.\mvnw spring-boot:run -Dspring-boot.run.profiles=mysql
```

---

## Проверка работы

Откройте в браузере:

```
http://localhost:8080
```

