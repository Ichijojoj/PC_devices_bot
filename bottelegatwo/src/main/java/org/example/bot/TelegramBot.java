package org.example.bot;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.sql.*;
import java.util.*;

public class TelegramBot extends TelegramLongPollingBot {
    private static final String BOT_USERNAME = " ";
    private static final String BOT_TOKEN = " ";
    private static final String DB_URL = " ";
    private static final String DB_USER = " ";
    private static final String DB_PASSWORD = " ";

    private Map<String, List<String>> pendingOrders = new HashMap<>();
    private Set<String> usersPendingUsername = new HashSet<>();

    @Override
    public String getBotUsername() {
        return BOT_USERNAME;
    }

    @Override
    public String getBotToken() {
        return BOT_TOKEN;
    }

    public boolean checkDBConnection() {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;

        try {

            connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);


            String query = "SELECT COUNT(*) FROM public.Manufacturer";;
            statement = connection.prepareStatement(query);
            resultSet = statement.executeQuery();

            if (resultSet.next()) {
                int count = resultSet.getInt(1);
                if (count > 0) {
                    System.out.println("База данных содержит данные.");
                    return true;
                } else {
                    System.out.println("База данных пуста.");
                    return false;
                }
            } else {
                System.out.println("Ошибка при выполнении запроса.");
                return false;
            }
        } catch (SQLException e) {

            System.out.println("Ошибка при подключении к базе данных: " + e.getMessage());
            return false;
        } finally {
            // Закрываем ресурсы
            if (resultSet != null) {
                try {
                    resultSet.close();
                } catch (SQLException e) {
                    System.out.println("Ошибка при закрытии ResultSet: " + e.getMessage());
                }
            }
            if (statement != null) {
                try {
                    statement.close();
                } catch (SQLException e) {
                    System.out.println("Ошибка при закрытии Statement: " + e.getMessage());
                }
            }
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    System.out.println("Ошибка при закрытии соединения: " + e.getMessage());
                }
            }
        }
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            System.out.println("привет1");
            String chatId = update.getMessage().getChatId().toString();
            String text = update.getMessage().getText();

            if (usersPendingUsername.contains(chatId)) {
                System.out.println("привет2");
                addUserIfNotExists(text, chatId);
                usersPendingUsername.remove(chatId);
                sendMenu(chatId);
            } else {
                System.out.println("привет3");
                String username = getUsernameFromChatId(chatId);

                if (username == null) {
                    System.out.println("привет4");
                    requestUsername(chatId); // Запрашиваем имя пользователя
                } else {
                    System.out.println("привет5");
                    if (pendingOrders.containsKey(chatId)) {
                        System.out.println(checkDBConnection());
                        handleQuantityInput(chatId, text);
                    } else {
                        handleUserInput(chatId, text);
                    }
                }
            }
        } else if (update.hasCallbackQuery()) {
            String chatId = update.getCallbackQuery().getMessage().getChatId().toString();
            String callbackData = update.getCallbackQuery().getData();

            if (callbackData.startsWith("item_")) {
                String itemId = callbackData.split("_")[1];
                requestQuantity(chatId, itemId);
            } else if (callbackData.startsWith("delivery_")) {
                System.out.println("Delivery1");
                String pointId = callbackData.split("_")[1];
                placeOrder(chatId, pointId);
            } else if (callbackData.equals("add_more_items")) {
                sendCategories(chatId);
            } else if (callbackData.equals("finalize_order")) {
                System.out.println("Final order11");
                requestDeliveryPoint(chatId);
            } else if (callbackData.startsWith("manufacturer_")) {
                int manufacturerId = Integer.parseInt(callbackData.split("_")[1]);
                handleManufacturerSelection(chatId, manufacturerId);
            } else if (callbackData.startsWith("order_")) {
                if (callbackData.equals("order_back")) {
                    handleMyOrders(chatId); // Вернуться к списку заказов
                } else if (callbackData.startsWith("order_delete_")) {
                    int orderId = Integer.parseInt(callbackData.split("_")[2]);
                    deleteOrder(chatId, orderId);
                } else {
                    int orderId = Integer.parseInt(callbackData.split("_")[1]);
                    handleOrderAction(chatId, orderId);
                }
            } else {
                handleCategorySelection(chatId, callbackData);
            }
        }
    }


    private void addUserIfNotExists(String username, String chatId) {
        if (username == null || username.isEmpty() || chatId == null || chatId.isEmpty()) {
            System.out.println("проверка1");
            return;
        }

        String queryCheck = "SELECT id FROM public.customers WHERE username = ?";
        String queryInsert = "INSERT INTO public.customers (id, username) VALUES (?, ?)";
        ResultSet rs = null;

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement checkStmt = conn.prepareStatement(queryCheck);
             PreparedStatement insertStmt = conn.prepareStatement(queryInsert)) {

            checkStmt.setString(1, username);
            rs = checkStmt.executeQuery();
            if (rs.next()) {
                System.out.println("проверка2");
                int userId = rs.getInt("id");
                System.out.println("User already exists with ID: " + userId);
            } else {
                System.out.println("проверка3");
                int id = Integer.parseInt(chatId); // Преобразование chatId в целое число
                insertStmt.setInt(1, id);
                insertStmt.setString(2, username);
                int rowsAffected = insertStmt.executeUpdate();
                if (rowsAffected == 1) {
                    System.out.println("Inserted new user with ID: " + chatId);
                } else {
                    System.out.println("Failed to insert new user.");
                }
            }
        } catch (SQLException e) {
            System.out.println("проверка4");
            throw new RuntimeException(e);
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException e) {
                    System.out.println("Error closing ResultSet: " + e.getMessage());
                }
            }
        }
    }
    private void handleMyOrders(String chatId) {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT o.id, o.Item_id, o.quantity, o.statys, o.price FROM Orders o " +
                             "JOIN Customers c ON o.Customer_id = c.id " +
                             "WHERE c.id = (SELECT id FROM Customers WHERE CAST(id AS TEXT) = ?)")) {

            stmt.setString(1, chatId);
            ResultSet rs = stmt.executeQuery();

            List<List<InlineKeyboardButton>> rows = new ArrayList<>();
            boolean hasOrders = false;

            while (rs.next()) {
                hasOrders = true;
                int orderId = rs.getInt("id");
                int itemId = rs.getInt("Item_id");
                int quantity = rs.getInt("quantity");
                String status = rs.getString("statys");
                double price = rs.getDouble("price");

                InlineKeyboardButton button = new InlineKeyboardButton();
                button.setText("Заказ " + orderId + ": Товар " + itemId + ", Кол-во: " + quantity + ", Статус: " + status + ", Стоимость: " + price + " USD");
                button.setCallbackData("order_" + orderId);
                List<InlineKeyboardButton> row = new ArrayList<>();
                row.add(button);
                rows.add(row);
            }

            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(chatId);

            if (hasOrders) {
                InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
                markup.setKeyboard(rows);
                sendMessage.setText("Ваши заказы:");
                sendMessage.setReplyMarkup(markup);
            } else {
                sendMessage.setText("У вас нет заказов.");
            }

            this.execute(sendMessage);

        } catch (SQLException | TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    private void handleOrderAction(String chatId, int orderId) {
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("НАЗАД");
        backButton.setCallbackData("order_back");

        InlineKeyboardButton deleteButton = new InlineKeyboardButton();
        deleteButton.setText("УДАЛИТЬ");
        deleteButton.setCallbackData("order_delete_" + orderId);

        List<InlineKeyboardButton> row1 = new ArrayList<>();
        row1.add(backButton);

        List<InlineKeyboardButton> row2 = new ArrayList<>();
        row2.add(deleteButton);

        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        rows.add(row1);
        rows.add(row2);

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(rows);

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText("Выберите действие для заказа " + orderId + ":");
        sendMessage.setReplyMarkup(markup);

        try {
            this.execute(sendMessage);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    private void deleteOrder(String chatId, int orderId) {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM Orders WHERE id = ?")) {

            stmt.setInt(1, orderId);
            int rowsAffected = stmt.executeUpdate();

            if (rowsAffected > 0) {
                sendTextMessage(chatId, "Заказ " + orderId + "был удален.");
            } else {
                sendTextMessage(chatId, "Ошибка: Заказ " + orderId + "не найден.");
            }
            handleMyOrders(chatId); // Показать обновленный список заказов

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void handleUserInput(String chatId, String text) {
        switch (text) {
            case "/start":
                System.out.println(checkDBConnection());
                String username = getUsernameFromChatId(chatId);
                if (username == null) {
                    requestUsername(chatId); // Запрашиваем имя пользователя
                } else {
                    sendMenu(chatId);
                }
                break;
            case "Товары по категориям":
                sendCategories(chatId);
                break;
            case "Поиск по Производителю":
                sendManufacturers(chatId);
                break;
            case "Мои Заказы":
                handleMyOrders(chatId);
                break;
            default:
                sendTextMessage(chatId, "Непонятная команда. Пожалуйста, используйте кнопки меню.");
                break;
        }
    }

    private void sendManufacturers(String chatId) {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement stmt = conn.prepareStatement("SELECT id, Manufacturer_name FROM public.Manufacturer")) {

            ResultSet rs = stmt.executeQuery();
            List<List<InlineKeyboardButton>> rows = new ArrayList<>();

            while (rs.next()) {
                InlineKeyboardButton button = new InlineKeyboardButton();
                button.setText(rs.getString("Manufacturer_name"));
                button.setCallbackData("manufacturer_" + rs.getInt("id"));
                List<InlineKeyboardButton> row = new ArrayList<>();
                row.add(button);
                rows.add(row);
            }

            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            markup.setKeyboard(rows);

            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(chatId);
            sendMessage.setText("Выберите производителя:");
            sendMessage.setReplyMarkup(markup);

            this.execute(sendMessage);
        } catch (SQLException | TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    private void handleManufacturerSelection(String chatId, int manufacturerId) {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);) {

            String manufacturerName = "";
            String manufacturerCountry = "";
            try (PreparedStatement manufacturerStmt = conn.prepareStatement(
                    "SELECT Manufacturer_name, Country FROM public.Manufacturer WHERE id = ?")) {
                manufacturerStmt.setInt(1, manufacturerId);
                try (ResultSet rs = manufacturerStmt.executeQuery()) {
                    if (rs.next()) {
                        manufacturerName = rs.getString("Manufacturer_name");
                        manufacturerCountry = rs.getString("Country");
                    }
                }
            }

            try (PreparedStatement productStmt = conn.prepareStatement(
                    "SELECT id, Item_name, Description, Price FROM public.Items WHERE Manufacturer_id = ?")) {
                productStmt.setInt(1, manufacturerId);
                try (ResultSet rs = productStmt.executeQuery()) {

                    SendMessage sendMessage = new SendMessage();
                    sendMessage.setChatId(chatId);
                    sendMessage.setText(String.format("Товары от выбранного производителя:\n\n%s, %s",
                            manufacturerName, manufacturerCountry));

                    InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
                    List<List<InlineKeyboardButton>> rows = new ArrayList<>();

                    while (rs.next()) {
                        InlineKeyboardButton button = new InlineKeyboardButton();
                        button.setText(rs.getString("Item_name") + " - " + rs.getDouble("Price") + " USD");
                        button.setCallbackData("item_" + rs.getInt("id"));
                        List<InlineKeyboardButton> row = new ArrayList<>();
                        row.add(button);
                        rows.add(row);
                    }

                    markup.setKeyboard(rows);
                    sendMessage.setReplyMarkup(markup);

                    this.execute(sendMessage);
                }
            }

        } catch (SQLException | TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }
    private void sendAddMoreOrFinalize(String chatId) {
        InlineKeyboardButton addMoreButton = new InlineKeyboardButton();
        addMoreButton.setText("Добавить еще товары");
        addMoreButton.setCallbackData("add_more_items");

        InlineKeyboardButton finalizeButton = new InlineKeyboardButton();
        finalizeButton.setText("Оформить заказ");
        finalizeButton.setCallbackData("finalize_order");

        List<InlineKeyboardButton> row1 = new ArrayList<>();
        row1.add(addMoreButton);

        List<InlineKeyboardButton> row2 = new ArrayList<>();
        row2.add(finalizeButton);

        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        rows.add(row1);
        rows.add(row2);

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(rows);

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText("Выберите действие:");
        sendMessage.setReplyMarkup(markup);

        try {
            this.execute(sendMessage);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    private void handleQuantityInput(String chatId, String text) {
        try {
            int quantity = Integer.parseInt(text);

            // Проверка на ноль
            if (quantity <= 0) {
                sendTextMessage(chatId, "Ввод количества отменен."); // Отменяем ввод количества
                sendMenu(chatId); // Выводим меню

                // Удаляем информацию о товаре из pendingOrders
                if (pendingOrders.containsKey(chatId)) {

                    System.out.println("должно работать");
                    pendingOrders.get(chatId).remove(pendingOrders.get(chatId).size() - 1);
                    sendAddMoreOrFinalize(chatId);
                }

                return;
            }

            List<String> orderInfoList = pendingOrders.get(chatId);
            String lastOrderInfo = orderInfoList.get(orderInfoList.size() - 1);
            String[] orderInfo = lastOrderInfo.split("_");
            String itemId = orderInfo[0];

            // Проверка количества в базе данных
            int availableQuantity = getAvailableQuantity(itemId);

            if (quantity <= availableQuantity) {
                // Вычитаем количество из базы данных
                if (updateItemQuantity(itemId, quantity)) {
                    // Обновляем количество в последнем добавленном товаре
                    orderInfoList.set(orderInfoList.size() - 1, itemId + "_" + quantity);

                    // Спрашиваем пользователя, хочет ли он добавить еще товаров или оформить заказ
                    sendAddMoreOrFinalize(chatId);
                } else {
                    sendTextMessage(chatId, "Ошибка при обновлении количества товара. Попробуйте позже.");
                }
            } else {
                sendTextMessage(chatId, "Недостаточно товара в наличии. Пожалуйста, введите меньшее количество.");
            }
        } catch (NumberFormatException e) {
            sendTextMessage(chatId, "Пожалуйста, введите допустимое количество.");
        }
    }
    // Функция для получения доступного количества товара
    private int getAvailableQuantity(String itemId) {
        int availableQuantity = 0;
        String query = "SELECT quantity FROM public.Items WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, Integer.parseInt(itemId));
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                availableQuantity = rs.getInt("quantity");
            }
        } catch (SQLException e) {
            System.out.println("Ошибка при получении доступного количества: " + e.getMessage());
        }
        return availableQuantity;
    }

    // Функция для обновления количества товара
    private boolean updateItemQuantity(String itemId, int quantity) {
        String queryUpdateItemQuantity = "UPDATE Items SET quantity = quantity - ? WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement stmtUpdateItemQuantity = conn.prepareStatement(queryUpdateItemQuantity)) {

            stmtUpdateItemQuantity.setInt(1, quantity);
            stmtUpdateItemQuantity.setInt(2, Integer.parseInt(itemId));
            int rowsAffected = stmtUpdateItemQuantity.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.out.println("Ошибка при обновлении количества товара: " + e.getMessage());
            return false;
        }
    }


    private void placeOrder(String chatId, String pointId) {
        System.out.println("Place1");
        String username = getUsernameFromChatId(chatId);
        String queryGetUserId = "SELECT id FROM public.customers WHERE username = ?";
        String queryInsertOrder = "INSERT INTO public.Orders (Customer_id, Item_id, Point_id, quantity, statys, price) VALUES (?, ?, ?, ?, 'В обработке', ?)";
        String queryGetItemDetails = "SELECT Item_name, Price FROM public.Items WHERE id = ?";
        System.out.println("Place2");

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement stmtGetUserId = conn.prepareStatement(queryGetUserId);
             PreparedStatement stmtInsertOrder = conn.prepareStatement(queryInsertOrder, Statement.RETURN_GENERATED_KEYS);
             PreparedStatement stmtGetItemDetails = conn.prepareStatement(queryGetItemDetails)) {

            // Get user ID
            stmtGetUserId.setString(1, username);
            ResultSet rs = stmtGetUserId.executeQuery();
            if (rs.next()) {
                System.out.println("Place3");
                int userId = rs.getInt("id");

                // Insert each item in the pending order
                List<String> orderInfoList = pendingOrders.get(chatId);
                StringBuilder orderDetails = new StringBuilder("Ваш заказ успешно оформлен!\n");
                System.out.println("Place4");

                boolean orderSuccessful = true; // Flag to track if the order is successful
                double totalOrderPrice = 0; // Variable to track the total order price

                for (String orderInfoString : orderInfoList) {
                    System.out.println("Place5");
                    String[] orderInfo = orderInfoString.split("_");
                    String itemId = orderInfo[0];
                    int quantity = Integer.parseInt(orderInfo[1]);

                    // Get item details
                    stmtGetItemDetails.setInt(1, Integer.parseInt(itemId));
                    ResultSet itemRs = stmtGetItemDetails.executeQuery();
                    if (itemRs.next()) {
                        String itemName = itemRs.getString("Item_name");
                        double itemPrice = itemRs.getDouble("Price");
                        orderDetails.append(String.format("Товар: %s, Количество: %d, Цена: %.2f USD\n", itemName, quantity, itemPrice));

                        totalOrderPrice += itemPrice * quantity; // Calculate item price and add to total

                        // Insert order
                        stmtInsertOrder.setInt(1, userId);
                        stmtInsertOrder.setInt(2, Integer.parseInt(itemId));
                        stmtInsertOrder.setInt(3, Integer.parseInt(pointId));
                        stmtInsertOrder.setInt(4, quantity);
                        stmtInsertOrder.setDouble(5, itemPrice * quantity); // Set the price for the order
                        stmtInsertOrder.addBatch();
                    } else {
                        // Item not found
                        orderSuccessful = false;
                        orderDetails.append(String.format("Товар: %s не найден. \n", itemId));
                        break; // Stop processing the order if the item is not found
                    }
                }

                if (orderSuccessful) {
                    int[] rowsAffected = stmtInsertOrder.executeBatch();

                    if (rowsAffected.length > 0) {
                        orderDetails.append(String.format("Общая сумма заказа: %.2f USD\n", totalOrderPrice)); // Add total price to message
                        sendTextMessage(chatId, orderDetails.toString());
                    } else {
                        System.out.println("Place6");
                        sendTextMessage(chatId, "Ошибка при оформлении заказа.");
                    }
                    pendingOrders.remove(chatId); // Clear pending orders for this user
                } else {
                    sendTextMessage(chatId, "Ошибка: заказ не оформлен. Проверьте количество доступных товаров.");
                    pendingOrders.remove(chatId); // Clear pending orders for this user
                }
            } else {
                System.out.println("Place7");
                sendTextMessage(chatId, "Ошибка: пользователь не найден.");
            }
        } catch (SQLException e) {
            System.out.println("Ошибка =" + e);
            throw new RuntimeException(e);
        }
    }

    private void requestDeliveryPoint(String chatId) {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement stmt = conn.prepareStatement("SELECT id, Adress FROM public.Point")) {

            ResultSet rs = stmt.executeQuery();
            List<List<InlineKeyboardButton>> rows = new ArrayList<>();

            while (rs.next()) {
                InlineKeyboardButton button = new InlineKeyboardButton();
                button.setText(rs.getString("Adress"));
                button.setCallbackData("delivery_" + rs.getInt("id"));
                List<InlineKeyboardButton> row = new ArrayList<>();
                row.add(button);
                rows.add(row);
            }

            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            markup.setKeyboard(rows);

            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(chatId);
            sendMessage.setText("Выберите пункт выдачи:");
            sendMessage.setReplyMarkup(markup);

            this.execute(sendMessage);
        } catch (SQLException | TelegramApiException e) {
            System.out.println("Ошибка =" + e);
            throw new RuntimeException(e);
        }
    }




    private void requestQuantity(String chatId, String itemId) {
        sendTextMessage(chatId, "Пожалуйста, введите количество товара:");
        pendingOrders.putIfAbsent(chatId, new ArrayList<>());
        pendingOrders.get(chatId).add(itemId + "_0"); // 0 временное количество
    }


    private void sendCategories(String chatId) {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement stmt = conn.prepareStatement("SELECT id, Category_name FROM public.Category")) {

            ResultSet rs = stmt.executeQuery();
            List<List<InlineKeyboardButton>> rows = new ArrayList<>();

            while (rs.next()) {
                InlineKeyboardButton button = new InlineKeyboardButton();
                button.setText(rs.getString("Category_name"));
                button.setCallbackData("category_" + rs.getInt("id"));
                List<InlineKeyboardButton> row = new ArrayList<>();
                row.add(button);
                rows.add(row);
            }

            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            markup.setKeyboard(rows);

            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(chatId);
            sendMessage.setText("Выберите категорию:");
            sendMessage.setReplyMarkup(markup);

            this.execute(sendMessage);

        } catch (SQLException | TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    private void handleCategorySelection(String chatId, String category) {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT id, Item_name, Description, Price FROM public.Items WHERE Category_id = ?")) {

            int categoryId = Integer.parseInt(category.split("_")[1]);
            stmt.setInt(1, categoryId);
            ResultSet rs = stmt.executeQuery();

            List<List<InlineKeyboardButton>> rows = new ArrayList<>();

            while (rs.next()) {
                InlineKeyboardButton button = new InlineKeyboardButton();
                button.setText(rs.getString("Item_name") + " - " + rs.getDouble("Price") + " USD");
                button.setCallbackData("item_" + rs.getInt("id"));
                List<InlineKeyboardButton> row = new ArrayList<>();
                row.add(button);
                rows.add(row);
            }

            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            markup.setKeyboard(rows);

            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(chatId);
            sendMessage.setText("Выберите товар:");
            sendMessage.setReplyMarkup(markup);

            this.execute(sendMessage);

        } catch (SQLException | TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    private void requestUsername(String chatId) {
        sendTextMessage(chatId, "Пожалуйста, введите ваше имя пользователя:");
        usersPendingUsername.add(chatId);
    }

    private void sendMenu(String chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Выберите действие из меню:");

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        row1.add(new KeyboardButton("Товары по категориям"));
        row1.add(new KeyboardButton("Поиск по Производителю"));

        KeyboardRow row2 = new KeyboardRow();
        row2.add(new KeyboardButton("Мои Заказы"));

        keyboard.add(row1);
        keyboard.add(row2);

        keyboardMarkup.setKeyboard(keyboard);
        message.setReplyMarkup(keyboardMarkup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    private void sendTextMessage(String chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    private String getUsernameFromChatId(String chatId) {
        System.out.println("Hi1");
        String query = "SELECT username FROM public.customers WHERE id = ?";
        System.out.println("Hi2");
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement stmt = conn.prepareStatement(query)) {
            System.out.println("Hi3");
            // Преобразование chatId в целое число
            int id = Integer.parseInt(chatId);
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                System.out.println("Hi4");
                return rs.getString("username");
            }
        } catch (SQLException e) {
            System.out.println("Hi5");
            System.out.println("Ошибка ="+ e);
            throw new RuntimeException(e);
        }
        System.out.println("Hi6");
        return null;
    }
}