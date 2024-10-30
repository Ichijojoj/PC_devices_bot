package org.example;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class DatabaseManager {










    public static void main(String[] args) {
        String url = "jdbc:sqlite:shop.db"; // Название файла базы данных


        // SQL-запросы для создания таблиц
        String sqlCreateTables = """
           CREATE TABLE Покупатели (
               id SERIAL PRIMARY KEY,
               username VARCHAR(100) NOT NULL
           );

            CREATE TABLE IF NOT EXISTS Товары (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                название VARCHAR(255) NOT NULL,
                описание TEXT,
                цена DECIMAL(10, 2) NOT NULL,
                производитель_id INTEGER,
                склад_id INTEGER,
                категория_id INTEGER,
                FOREIGN KEY (производитель_id) REFERENCES Производители(id),
                FOREIGN KEY (склад_id) REFERENCES Склад(id),
                FOREIGN KEY (категория_id) REFERENCES Категории(id)
            );

            CREATE TABLE Заказы (
                  id INTEGER PRIMARY KEY AUTOINCREMENT,
                  покупатель_id INT REFERENCES Покупатели(id),
              	  пункт_выдачи_id INT REFERENCES Пункты_выдачи(id),
                  товар_id INT REFERENCES Товары(id),
                  количество INT NOT NULL,
                  статус VARCHAR(50) DEFAULT 'В обработке'
              );
              

            CREATE TABLE IF NOT EXISTS Пункты_выдачи (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                адрес VARCHAR(255) NOT NULL,
                время_работы VARCHAR(100)
            );

            CREATE TABLE IF NOT EXISTS Производители (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                название VARCHAR(255) NOT NULL,
                страна VARCHAR(100)
            );

            CREATE TABLE IF NOT EXISTS Склад (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                название VARCHAR(255) NOT NULL,
                местоположение VARCHAR(255)
            );

            CREATE TABLE IF NOT EXISTS Категории (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                название VARCHAR(255) NOT NULL
            );
        """;


        String sqlInsertData = """
                               INSERT INTO Категории (название) VALUES
                               ('Жесткие диски'),
                               ('Ноутбуки'),
                               ('Мониторы'),
                               ('Серверы'),
                               ('Видеокарты'),
                               ('Телевизоры'),
                               ('Маршрутизаторы'),
                               ('Материнские платы'),
                               ('Оперативная память'),
                               ('USB накопители'),
                               ('Процессоры');

            INSERT INTO Производители (название, страна) VALUES
            ('Intel Corporation', 'США'),
            ('Samsung Electronics Co., Ltd.', 'Южная Корея'),
            ('ASUS', 'Тайвань'),
            ('Western Digital Corporation', 'США'),
            ('Dell Technologies Inc.', 'США'),
            ('Seagate Technology plc', 'Ирландия'),
            ('MSI (Micro-Star International Co., Ltd.)', 'Тайвань'),
            ('Kingston Technology Company, Inc.', 'США');

            INSERT INTO Склад (название, местоположение) VALUES
            ('Склад 1', 'ул. Складская, 10'),
            ('Склад 2', 'пр. Производственный, 20'),
            ('Склад 3', 'ул. Логистическая, 5'),
            ('Склад 4', 'пр. Транспортный, 15'),
            ('Склад 5', 'ул. Распределительная, 30');

            INSERT INTO Пункты_выдачи (адрес, время_работы) VALUES
            ('ул. Ленина, 10', 'Пн-Пт: 9:00-18:00, Сб: 10:00-14:00'),
            ('пр. Победы, 20', 'Пн-Пт: 10:00-20:00, Сб-Вс: 11:00-16:00'),
            ('ул. Советская, 5', 'Ежедневно: 8:00-22:00'),
            ('пр. Гагарина, 15', 'Пн-Пт: 9:00-17:00'),
            ('ул. Кирова, 30', 'Пн-Сб: 8:00-19:00'),
            ('пр. Ленинградский, 25', 'Ежедневно: 10:00-18:00');
            
            -- Добавление товаров от компании Intel Corporation
            INSERT INTO Товары (название, описание, цена, производитель_id, склад_id, категория_id) VALUES
            ('Процессор Intel Core i9-10900K', 'Мощный процессор для высокопроизводительных компьютеров', 499.99, 1, 1, 'Процессоры'),
            ('SSD накопитель Intel SSD 660p Series', 'Быстрый и емкий SSD для ускорения загрузки и работы системы', 89.99, 1, 2, 'Жесткие диски'),
            ('Материнская плата Intel Z490', 'Прочная и надежная материнская плата для сборки ПК', 199.99, 1, 3, 'Материнские платы');
            
            
            
            -- Добавление товаров от компании Samsung Electronics Co., Ltd.
            INSERT INTO Товары (название, описание, цена, производитель_id, склад_id, категория_id) VALUES
            ('Монитор Samsung Odyssey G7', 'Игровой монитор с высокой частотой обновления и качественным изображением', 599.99, 2, 1, 'Мониторы'),
            ('Накопитель SSD Samsung 970 EVO Plus', 'Быстрый SSD для повышения производительности ПК', 129.99, 2, 2, 'Жесткие диски'),
            ('Телевизор Samsung QLED 4K Q90T', 'Высококачественный телевизор с ярким и четким изображением', 1499.99, 2, 3, 'Телевизоры');
            
            -- Добавление товаров от компании ASUS
            INSERT INTO Товары (название, описание, цена, производитель_id, склад_id, категория_id) VALUES
            ('Видеокарта ASUS GeForce RTX 3080', 'Мощная видеокарта для игр и графических приложений', 899.99, 3, 1, 'Видеокарты'),
            ('Материнская плата ASUS ROG Strix Z490-E Gaming', 'Профессиональная материнская плата для геймеров и оверклокеров', 299.99, 3, 2, 'Материнские платы'),
            ('Маршрутизатор ASUS RT-AX86U', 'Быстрый и надежный маршрутизатор для домашней сети', 249.99, 3, 3, 'Маршрутизаторы');
            
            
            
            -- Добавление товаров от компании Western Digital Corporation
            INSERT INTO Товары (название, описание, цена, производитель_id, склад_id, категория_id) VALUES
            ('Жесткий диск WD Blue', 'Емкий и надежный HDD для хранения данных', 79.99, 4, 1, 'Жесткие диски'),
            ('SSD накопитель WD Black SN750 NVMe', 'Высокоскоростной SSD для игр и работы с данными', 149.99, 4, 2, 'Жесткие диски'),
            ('Внешний жесткий диск WD Elements', 'Удобное решение для переноса и хранения больших объемов данных', 99.99, 4, 3, 'Жесткие диски');
            
            -- Добавление товаров от компании Dell Technologies Inc.
            INSERT INTO Товары (название, описание, цена, производитель_id, склад_id, категория_id) VALUES
            ('Ноутбук Dell XPS 15', 'Мощный и компактный ноутбук для работы и развлечений', 1499.99, 5, 1, 'Ноутбуки'),
            ('Монитор Dell UltraSharp U2720Q', 'Профессиональный монитор с высоким разрешением для работы с графикой', 699.99, 5, 2, 'Мониторы'),
            ('Сервер Dell PowerEdge R640', 'Надежный сервер для бизнес-приложений и облачных сервисов', 2999.99, 5, 3, 'Серверы');
            
            -- Добавление товаров от компании Seagate Technology plc
            INSERT INTO Товары (название, описание, цена, производитель_id, склад_id, категория_id) VALUES
            ('Жесткий диск Seagate Barracuda', 'Надежный и быстрый HDD для хранения данных', 69.99, 6, 1, 'Жесткие диски'),
            ('SSD накопитель Seagate FireCuda 510 NVMe', 'Быстрый SSD с высокой производительностью для игр', 129.99, 6, 2, 'Жесткие диски'),
            ('Внешний HDD Seagate Backup Plus Portable', 'Компактное и надежное решение для резервного копирования данных', 89.99, 6, 3, 'Жесткие диски');
            
            -- Добавление товаров от компании MSI (Micro-Star International Co., Ltd.)
            INSERT INTO Товары (название, описание, цена, производитель_id, склад_id, категория_id) VALUES
            ('Видеокарта MSI GeForce RTX 3070 Gaming X Trio', 'Мощная видеокарта для игр и видеомонтажа', 699.99, 7, 1, 'Видеокарты'),
            ('Материнская плата MSI MAG B460M Mortar', 'Компактная и функциональная материнская плата для среднего ПК', 99.99, 7, 2, 'Материнские платы'),
            ('Ноутбук MSI GS66 Stealth', 'Геймерский ноутбук с мощным железом и стильным дизайном', 1499.99, 7, 3, 'Ноутбуки');
            
            -- Добавление товаров от компании Kingston Technology Company, Inc.
            INSERT INTO Товары (название, описание, цена, производитель_id, склад_id, категория_id) VALUES
            ('Оперативная память Kingston HyperX Fury', 'Высокопроизводительная и надежная оперативная память для игр и приложений', 79.99, 8, 1, 'Оперативная память'),
            ('USB флеш-накопитель Kingston DataTraveler', 'Надежное и быстрое устройство для хранения и передачи данных', 19.99, 8, 2, 'USB накопители'),
            ('SSD накопитель Kingston A2000 NVMe', 'Быстрый и надежный SSD для повышения производительности ПК', 109.99, 8, 3, 'Жесткие диски');
            
            
            UPDATE Товары
            SET категория_id = (SELECT id FROM Категории WHERE Категории.название = Товары.категория_id);
        """;

        try (Connection conn = DriverManager.getConnection(url);
             Statement stmt = conn.createStatement()) {

            // Создание таблиц
            stmt.executeUpdate(sqlCreateTables);

            // Заполнение таблиц данными
            stmt.executeUpdate(sqlInsertData);

            System.out.println("База данных и таблицы успешно созданы и заполнены.");

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
