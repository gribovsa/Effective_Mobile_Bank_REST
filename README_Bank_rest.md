<h2>💳Система управления банковскими картами</h2>

<p>
    Это REST-приложение на Spring Boot v 4.0.1 (актуально декабрь 2025) для управления банковскими картами, использующее PostgreSQL в качестве базы данных 
    и Liquibase для миграций базы данных. Приложение контейнеризировано с помощью Docker и управляется через Docker Compose. 
    API документирован с использованием Springdoc OpenAPI и доступен через Swagger UI.
</p>

<h2>⚒️Требования</h2>
<p>Перед запуском приложения убедитесь, что у вас установлены:</p>
<ul>
    <li>Docker: Необходим для сборки и запуска контейнеров приложения.</li>
    <li>Docker Compose: Используется для управления несколькими контейнерами.</li>
    <li>Maven: Требуется для сборки JAR-файла приложения.</li>
    <li>Java 22: Приложение собрано с использованием Java 22.</li>
    <li>pgAdmin (опционально): Для проверки базы данных PostgreSQL.</li>
</ul>

<h2>📂Структура проекта</h2>
<ul>
    <li>src/main/resources/application.yml: Конфигурация Spring Boot, включая настройки подключения к базе данных, Liquibase, JWT и Springdoc.</li>
    <li>src/main/resources/db/migration/changelog-master.yml: Основной файл Liquibase для управления миграциями базы данных.</li>
    <li>Dockerfile: Описывает Docker-образ для приложения Spring Boot.</li>
    <li>docker-compose.yml: Определяет сервисы (приложение и база данных) и их конфигурацию.</li>
    <li>pom.xml: Конфигурация Maven для зависимостей и сборки.</li>
</ul>

<h2>🚀Установка и запуск приложения</h2>
<p>Следуйте этим шагам для настройки и запуска приложения:</p>
<ul>
  <li>
    <strong>Клонируйте репозиторий:</strong>
    <pre><code>git clone https://github.com/gribovsa/Effective_Mobile_Bank_REST.git
cd Effective_Mobile_Bank_REST</code></pre>
  </li>

  <li>
    <strong>Соберите приложение:</strong>
    <p>Используйте Maven для компиляции и упаковки приложения в JAR-файл:</p>
    <code>mvn clean package</code>
    <p>Это создаст файл <em>target/bank-rest-0.0.1-SNAPSHOT.jar</em>.</p>
  </li>

  <li>
    <strong>Запустите приложение с помощью Docker Compose:</strong>
    <p>Запустите приложение и базу данных PostgreSQL:</p>
    <code>docker-compose up --build</code>
    <ul>
      <li>Флаг <code>--build</code> обеспечивает пересборку Docker-образа приложения.</li>
      <li>Команда запускает два контейнера:
        <ul>
          <li><code>bank_rest-app-1</code>: Приложение Spring Boot на порту 8081.</li>
          <li><code>bank_rest-db-1</code>: База данных PostgreSQL на порту 5432.</li>
        </ul>
      </li>
    </ul>
  </li>

  <li>
    Проверьте, что приложение работает:
    <ul>
      <li>
        Проверьте логи, чтобы убедиться, что приложение и база данных запустились успешно:
        <br><code>docker logs bank_rest-app-1</code>
        <br><code>docker logs bank_rest-db-1</code>
      </li>
      <li>
        Ищите сообщения вроде:
        <br><code>app-1 | Started BankCardsApplication in X seconds</code>
        <br><code>db-1 | database system is ready to accept connections</code>
      </li>
    </ul>
  </li>


  <li>
    Проверьте API, отправив запрос на endpoint аутентификации (например, <code>/api/auth/login</code>):
    <pre><code>curl -X POST http://localhost:8081/api/auth/login \
-H "Content-Type: application/json" \
-d '{"username":"regular_user","password":"password"}'</code></pre>
  </li>

  <li>
    Используйте полученный JWT-токен для аутентифицированных запросов:
    <pre><code>curl -H "Authorization: Bearer ваш_jwt_токен" http://localhost:8081/api/some_endpoint</code></pre>
  </li>

  <li>
    Доступ к Swagger UI для просмотра документации API:
    <br><a href="http://localhost:8081/swagger-ui/index.html">http://localhost:8081/swagger-ui/index.html</a>
  </li>

  <li>
    Документация OpenAPI доступна по:
    <br><a href="http://localhost:8081/api-docs">http://localhost:8081/api-docs</a>
  </li>
</ul>


<h2>💾Доступ к базе данных (опционально)</h2>
<ul>
  <li>
    <strong>Проверка базы данных:</strong>
    <p>Подключитесь к контейнеру через команду psql:</p>
    <pre><code>docker exec -it bank_rest-db-1 psql -U postgres -d bankdb</code></pre>
    <p>Выполните SQL-команды для проверки таблиц и данных:</p>
    <pre><code>\dt
SELECT * FROM users;
SELECT * FROM cards;</code></pre>
  </li>

  <li>
    <strong>Настройка pgAdmin (localhost:5432):</strong>
    <ul>
      <li><strong>Имя пользователя:</strong> postgres</li>
      <li><strong>Пароль:</strong> root</li>
      <li><strong>База данных:</strong> bankdb</li>
    </ul>
  </li>

  <li>
    <strong>Остановка приложения:</strong>
    <p>Чтобы остановить и удалить контейнеры и тома, выполните:</p>
    <code>docker-compose down -v</code>
  </li>
</ul>

<h2>⚙️Детали конфигурации</h2>

<h3>База данных:</h3>
<ul>
    <li>
        <strong>Автоматизация и миграции:</strong>
        <p>База данных <code>bankdb</code> создается автоматически при запуске. 
        Liquibase применяет миграции из <code>changelog-master.yml</code> для создания таблиц (<code>users</code>, <code>cards</code>) и наполнения их данными.</p>
    </li>
    <li>
        <strong>Настройки подключения (application.yml):</strong>
        <pre><code>spring:
  datasource:
    url: jdbc:postgresql://db:5432/bankdb
    username: postgres
    password: root
    driver-class-name: org.postgresql.Driver</code></pre>
    </li>
</ul>

<h3>Приложение:</h3>
<ul>
    <li>
        <strong>🔐Аутентификация и порт:</strong>
        <p>Приложение Spring Boot работает на порту <code>8081</code>.</p>
        <p>Используется JWT со следующими параметрами:</p>
        <pre><code>jwt:
  secret: K7mN9pQ2vL8jR4tY5uI6oP1wQ3eA8xZ9oX7kP9mQ2vL8jR4tY
  expiration: 3600000 # 1 час</code></pre>
    </li>
    <li>
        <strong>📖Документация API:</strong>
        <p>Swagger UI и OpenAPI включены в конфигурации:</p>
        <pre><code>springdoc:
  api-docs:
    path: /api-docs
  swagger-ui:
    path: /swagger-ui
    enabled: true</code></pre>
    </li>
    <li>
        <strong>🐋Docker Compose:</strong>
        <p>Файл <code>docker-compose.yml</code> определяет два сервиса:</p>
        <ul>
            <li><code>app</code> (Spring Boot) — основной сервис приложения.</li>
            <li><code>db</code> (PostgreSQL) — база данных.</li>
            <li><strong>Особенности:</strong>
                <ul>
                    <li>Используется постоянный том (<code>pgdata</code>) для сохранения данных.</li>
                    <li>Настроена проверка <code>healthy</code>: приложение запустится только после готовности базы.</li>
                </ul>
            </li>
        </ul>
    </li>
</ul>


<h2>🩺Устранение неполадок</h2>
<ul>
  <li>
    <strong>Проблема: База данных не отображается в pgAdmin</strong>
    <ul>
      <li>Убедитесь, что pgAdmin настроен на <code>localhost:5432</code> с учетными данными <code>postgres</code> / <code>root</code>.</li>
      <li>Проверьте, не занят ли порт <code>5432</code>. При конфликте измените проброс в <code>docker-compose.yml</code> (например, <code>"5433:5432"</code>) и обновите настройки в pgAdmin.</li>
    </ul>
  </li>

  <li>
    <strong>Проблема: Ошибки с истекшими JWT-токенами</strong>
    <p>Если вы видите ошибку <code>JWT expired</code>, сгенерируйте новый токен:</p>
    <pre><code>curl -X POST http://localhost:8081/api/auth/login \
-H "Content-Type: application/json" \
-d '{"username":"user","password":"password"}'</code></pre>
    <p>Для увеличения срока жизни токена (например, до 24 часов) измените <code>application.yml</code>:</p>
    <pre><code>jwt:
  expiration: 86400000 # 24 часа</code></pre>
  </li>

  <li>
    <strong>Проблема: Предупреждения Maven (org.jetbrains:annotations)</strong>
    <p>Добавьте или обновите зависимость в <code>pom.xml</code>:</p>
    <pre><code>&lt;dependency&gt;
  &lt;groupId&gt;org.jetbrains&lt;/groupId&gt;
  &lt;artifactId&gt;annotations&lt;/artifactId&gt;
  &lt;version&gt;24.0.1&lt;/version&gt;
&lt;/dependency&gt;</code></pre>
  </li>

  <li>
    <strong>Проблема: Приложение не подключается к базе данных</strong>
    <ul>
      <li>Проверьте статус контейнера (должен быть <code>healthy</code>): <code>docker ps</code></li>
      <li><strong>Важно:</strong> Внутри Docker используется <code>jdbc:postgresql://db:5432/bankdb</code>, а для локального запуска (без Docker) — <code>jdbc:postgresql://localhost:5432/bankdb</code>.</li>
    </ul>
  </li>

  <li>
    <strong>Миграции и шифрование (справочно):</strong>
    <ul>
      <li><strong>Liquibase:</strong> Схема управляется через <code>changelog-master.yml</code>.</li>
      <li><strong>Encryption Key:</strong> Используется ключ из <code>application.yml</code>:
        <br><code>encryption.key: Kj8pLm9nQ2vX4yZ8aB5cD6eF7gH9iJ0k</code>
      </li>
    </ul>
  </li>
</ul>