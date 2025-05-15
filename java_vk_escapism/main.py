import vk_api
from vk_api.longpoll import VkLongPoll, VkEventType
from vk_api.keyboard import VkKeyboard, VkKeyboardColor
from vk_api import VkUpload

import random
import time
import re
import pyodbc
import requests
import io
import matplotlib.pyplot as plt
import datetime

# Параметры подключения к базе данных
DB_DRIVER = "ODBC Driver 17 for SQL Server"
DB_SERVER = "localhost\\SQLEXPRESS"
DB_NAME = "0bl1v1um_vk"

def get_db_connection():
    conn_str = (
        f"DRIVER={{{DB_DRIVER}}};"
        f"SERVER={DB_SERVER};"
        f"DATABASE={DB_NAME};"
        "Trusted_Connection=yes;"
    )
    return pyodbc.connect(conn_str)

# --- работа с БД (users, orders) ---

def update_user_state(vk_id, state):
    try:
        conn = get_db_connection()
        cursor = conn.cursor()
        merge_query = """
        MERGE dbo.users AS target
        USING (SELECT ? AS vk_id, ? AS last_state) AS source
           ON target.vk_id = source.vk_id
        WHEN MATCHED THEN 
            UPDATE SET last_state = source.last_state, last_interaction = GETDATE()
        WHEN NOT MATCHED THEN
            INSERT (vk_id, last_state, last_interaction)
            VALUES (source.vk_id, source.last_state, GETDATE());
        """
        cursor.execute(merge_query, (vk_id, state))
        conn.commit()
    except Exception as ex:
        print("Ошибка при обновлении пользователя:", ex)
    finally:
        conn.close()

def get_user_state(vk_id):
    try:
        conn = get_db_connection()
        cursor = conn.cursor()
        cursor.execute("SELECT last_state FROM dbo.users WHERE vk_id = ?", (vk_id,))
        row = cursor.fetchone()
        return row[0] if row else None
    except Exception as ex:
        print("Ошибка при получении состояния пользователя:", ex)
        return None
    finally:
        conn.close()

def save_order(vk_id, order_data, status='order_confirmed'):
    try:
        conn = get_db_connection()
        cursor = conn.cursor()
        cursor.execute(
            "INSERT INTO dbo.orders (user_vk_id, order_data, status) VALUES (?, ?, ?)",
            (vk_id, order_data, status)
        )
        conn.commit()
        cursor.execute("SELECT @@IDENTITY AS order_id")
        order_id = cursor.fetchone()[0]
        return order_id
    except Exception as ex:
        print("Ошибка при сохранении заказа:", ex)
        return None
    finally:
        conn.close()

def update_order_status(vk_id, new_status):
    try:
        conn = get_db_connection()
        cursor = conn.cursor()
        update_query = """
        UPDATE dbo.orders
        SET status = ?, updated_at = GETDATE()
        WHERE user_vk_id = ? AND id = (
            SELECT TOP 1 id FROM dbo.orders WHERE user_vk_id = ? ORDER BY created_at DESC
        )
        """
        cursor.execute(update_query, (new_status, vk_id, vk_id))
        conn.commit()
    except Exception as ex:
        print("Ошибка при обновлении статуса заказа:", ex)
    finally:
        conn.close()

def get_latest_order(vk_id):
    try:
        conn = get_db_connection()
        cursor = conn.cursor()
        query = """
        SELECT TOP 1 id, order_data, status, created_at
        FROM dbo.orders
        WHERE user_vk_id = ?
        ORDER BY created_at DESC
        """
        cursor.execute(query, (vk_id,))
        row = cursor.fetchone()
        if row:
            return {
                'order_id': row[0],
                'order_data': row[1],
                'status': row[2],
                'created_at': row[3]
            }
        return None
    except Exception as ex:
        print("Ошибка при получении заказа:", ex)
        return None
    finally:
        conn.close()

def add_order_note(order_id, note):
    """
    Добавляет заметку к заказу
    """
    try:
        conn = get_db_connection()
        cursor = conn.cursor()
        cursor.execute(
            "INSERT INTO dbo.order_notes (order_id, note, created_at) VALUES (?, ?, GETDATE())",
            (order_id, note)
        )
        conn.commit()
        return True
    except Exception as ex:
        print("Ошибка при добавлении заметки к заказу:", ex)
        return False
    finally:
        conn.close()

# --------------------------
# Настройки VK
TOKEN = 'vk1.a.M4XHZTCBKqaRbaakRPT2fMN9R8z-BNP4uCJKON_KTGXjPZd_e8Qq4-FPWbzIfod-u2PzQihTfCGkEQCDL_NexoWhFG55cHqlThWdLsHzkWQfoYAlSu4hO54BFNSs_V1S7fjxUUHw3ifjNWyIeMDCY0LRWLi8dwwAdOIuX3sT3UOfHl60Qa1nhWEfshICQ0eqaUNATzTxd9Ygi6Iwx4MT_Q'  # Ваш токен

vk_session = vk_api.VkApi(token=TOKEN)
vk = vk_session.get_api()
longpoll = VkLongPoll(vk_session)

# Для загрузки фотографий/картинок
uploader = VkUpload(vk_session)

# --------------------------
# Клавиатуры для бота (пример)
def get_main_keyboard():
    keyboard = VkKeyboard(one_time=False)
    keyboard.add_button("Оформить заказ", color=VkKeyboardColor.PRIMARY)
    keyboard.add_button("Инфо о заказе", color=VkKeyboardColor.POSITIVE)
    keyboard.add_line()
    keyboard.add_button("Связаться с админом", color=VkKeyboardColor.SECONDARY)
    keyboard.add_line()
    keyboard.add_button("Помощь", color=VkKeyboardColor.NEGATIVE)
    return keyboard

def get_order_management_keyboard():
    keyboard = VkKeyboard(one_time=False)
    keyboard.add_button("Инфо о заказе", color=VkKeyboardColor.POSITIVE)
    keyboard.add_line()
    keyboard.add_button("Смена данных/размера", color=VkKeyboardColor.PRIMARY)
    keyboard.add_button("Нет, отправил больше 2-х месяцев", color=VkKeyboardColor.SECONDARY)
    keyboard.add_line()
    keyboard.add_button("Возврат", color=VkKeyboardColor.NEGATIVE)
    keyboard.add_line()
    keyboard.add_button("Вернуться в меню", color=VkKeyboardColor.PRIMARY)
    return keyboard

def get_order_keyboard():
    keyboard = VkKeyboard(one_time=True)
    keyboard.add_button("Отмена", color=VkKeyboardColor.NEGATIVE)
    return keyboard

def get_admin_keyboard():
    keyboard = VkKeyboard(one_time=True)
    keyboard.add_button("Вернуться в меню", color=VkKeyboardColor.PRIMARY)
    return keyboard

def send_message(user_id, message, keyboard=None):
    vk.messages.send(
        user_id=user_id,
        message=message,
        random_id=random.randint(1, 10**6),
        keyboard=keyboard.get_keyboard() if keyboard else None
    )

# --------------------------
# Временное хранение состояний пользователей 
user_states = {}

def contains_market_product_link(message_text):
    pattern = r"https?://vk\.com/market/product/\d+-\d+-\d+"
    return re.search(pattern, message_text)

def contains_market_product_attachment(event):
    if hasattr(event, 'attachments') and event.attachments:
        for attach in event.attachments:
            if isinstance(attach, dict) and attach.get('type') == "market":
                return True
    return False

# --------------------------
# Функция: получить 7дн исторические данные + построить картинку
def create_token_chart(token_symbol: str):
    """
    Используем CoinGecko как пример:
    1. Ищем coin_id (упростим: inj -> injective-protocol, btc -> bitcoin и т.д.)
    2. Берём sparkline_7d
    3. Рисуем matplotlib график
    4. Возвращаем (img_bytes, current_price, change_24h, cmc_link)
    """
    # Упрощённая мапа символ -> coingecko ID
    mapping = {
        "inj": "injective-protocol",
        "btc": "bitcoin",
        "eth": "ethereum",
    }
    coin_id = mapping.get(token_symbol.lower(), token_symbol.lower())  # fallback

    url = f"https://api.coingecko.com/api/v3/coins/{coin_id}?localization=false&market_data=true&sparkline=true"
    resp = requests.get(url, timeout=10).json()
    if "error" in resp:
        raise ValueError(resp["error"])

    name = resp["name"]
    current_price = resp["market_data"]["current_price"]["usd"]
    price_change_24h = resp["market_data"]["price_change_percentage_24h"]
    spark7d = resp["market_data"]["sparkline_7d"]["price"]

    # Рисуем график
    plt.figure(figsize=(5,3), dpi=100)
    plt.plot(spark7d, label=f"{token_symbol.upper()} 7d")
    plt.title(f"{name} (7d)")
    plt.xlabel("Time →")
    plt.ylabel("Price (USD)")
    plt.legend(loc="best")

    buf = io.BytesIO()
    plt.savefig(buf, format="png")
    buf.seek(0)
    plt.close()

    # Для CoinMarketCap ссылки:
    # Можно по slug (Coingecko) -> "injective-protocol", но лучше "coinmarketcap.com/currencies/<slug>"
    # slug = resp.get("slug", coin_id)
    # Но coingecko slug != coinmarketcap slug, 
    # упрощённо: 
    cmc_link = f"https://coinmarketcap.com/currencies/{coin_id}/"

    return buf, current_price, price_change_24h, cmc_link, name

def upload_image_to_vk(image_bytes: io.BytesIO):
    """
    Загрузка картинки (PNG в памяти) на сервер VK,
    возврат attachment вида "photo{owner_id}_{media_id}".
    """
    # 1) Получаем URL для загрузки
    upload_url = vk.photos.getMessagesUploadServer()["upload_url"]
    # 2) Отправляем файл
    files = {"photo": ("chart.png", image_bytes, "image/png")}
    r = requests.post(upload_url, files=files)
    result = r.json()
    # 3) Сохраняем на серверах ВК
    saved_photo = vk.photos.saveMessagesPhoto(
        photo=result["photo"],
        server=result["server"],
        hash=result["hash"]
    )[0]  # вернется список
    # 4) Формируем attachment-строку
    attachment = f"photo{saved_photo['owner_id']}_{saved_photo['id']}"
    return attachment

def handle_token_message(message_text_full, user_id):
    """
    Если пользователь ввёл $TOKEN, берём цены, рисуем график,
    отправляем в чат.
    """
    # Например, строка "$INJ" -> token="inj"
    token = message_text_full.strip()[1:]  # убираем "$"
    token = token.lower()

    try:
        image_buf, price, change24, cmc_link, name = create_token_chart(token)
        # Загружаем картинку
        attachment = upload_image_to_vk(image_buf)
        # Формируем сообщение
        # VK не поддерживает "Embed", но мы сделаем форматированный текст
        msg = (f"{name} ({token.upper()})\n"
               f"Текущая цена: ${price:,.4f}\n"
               f"Изм. за 24ч: {change24:.2f}%\n"
               f"График за 7 дней.\n"
               f"Подробнее: {cmc_link}")

        # отправляем 
        vk.messages.send(
            user_id=user_id,
            message=msg,
            random_id=random.randint(1,10**6),
            attachment=attachment
        )

    except Exception as e:
        error_msg = f"Ошибка при получении графика для {token.upper()}: {e}"
        send_message(user_id, error_msg)

# --------------------------
# Основная логика обработки сообщений
def process_user_message(event):
    user_id = event.user_id
    message_text_full = event.text.strip()
    message_text = message_text_full.lower()

    # 1. Проверяем, не ввёл ли пользователь `$Токен`
    #    (e.g. "$inj", "$btc")
    if message_text_full.startswith("$") and len(message_text_full) > 1:
        handle_token_message(message_text_full, user_id)
        return

    # Далее идёт логика вашего магазина/заказов:
    current_state = get_user_state(user_id)
    if current_state is None:
        update_user_state(user_id, 'new')
        current_state = 'new'

    state_info = user_states.get(user_id, {'state': current_state})
    state = state_info.get('state')

    if state in ['ordering', 'change_order', 'return_order', 'admin_dialog', 'order_confirmed']:
        # ... тут не меняем, как раньше ...
        # (тот же код, что у вас уже есть)
        if state == 'ordering':
            if message_text == "отмена":
                update_user_state(user_id, 'new')
                user_states.pop(user_id, None)
                send_message(user_id, "Оформление заказа отменено.", get_main_keyboard())
            else:
                order_id = save_order(user_id, message_text_full, status='order_confirmed')
                update_user_state(user_id, 'order_confirmed')
                user_states[user_id] = {'state': 'order_confirmed', 'order_id': order_id}
                send_message(user_id,
                             f"Ваш заказ (ID {order_id}) принят:\n{message_text_full}\nМы свяжемся с вами.",
                             get_order_management_keyboard())
            return
        if state == 'admin_dialog':
            if message_text == "вернуться в меню":
                update_user_state(user_id, 'new')
                user_states.pop(user_id, None)
                send_message(user_id, "Возвращаемся в главное меню.", get_main_keyboard())
            else:
                send_message(user_id,
                             "Ваше сообщение для администратора отправлено. Ожидайте ответа.",
                             get_admin_keyboard())
            return
        if state == 'order_confirmed':
            if message_text == "смена данных/размера":
                update_user_state(user_id, 'change_order')
                user_states[user_id]['state'] = 'change_order'
                send_message(user_id,
                             "Введите новые данные или размер для изменения заказа. Для отмены введите \"Отмена\".",
                             get_order_keyboard())
                return
            elif message_text == "нет, отправил больше 2-х месяцев":
                update_order_status(user_id, 'delayed')
                send_message(user_id,
                             "Статус обновлен: заказ помечен как задержанный (более 2-х месяцев).",
                             get_order_management_keyboard())
                return
            elif message_text == "возврат":
                update_user_state(user_id, 'return_order')
                user_states[user_id]['state'] = 'return_order'
                send_message(user_id,
                             "Введите причину возврата и детали заказа. Для отмены введите \"Отмена\".",
                             get_order_keyboard())
                return
            elif message_text == "вернуться в меню":
                update_user_state(user_id, 'order_confirmed')
                user_states[user_id]['state'] = 'order_confirmed'
                send_message(user_id, "Возвращаемся в главное меню.", get_order_management_keyboard())
                return

            # обрабатываем change_order / return_order
            if state in ['change_order', 'return_order']:
                if message_text == "отмена":
                    update_user_state(user_id, 'order_confirmed')
                    user_states[user_id]['state'] = 'order_confirmed'
                    send_message(user_id, "Операция отменена.", get_order_management_keyboard())
                else:
                    if state == 'change_order':
                        # Обновляем данные заказа
                        order = get_latest_order(user_id)
                        if order:
                            order_id = order['order_id']
                            try:
                                conn = get_db_connection()
                                cursor = conn.cursor()
                                cursor.execute(
                                    "UPDATE dbo.orders SET order_data = ?, status = 'updated', updated_at = GETDATE() WHERE id = ?",
                                    (message_text_full, order_id)
                                )
                                conn.commit()
                                send_message(user_id, f"Заказ успешно обновлен:\n{message_text_full}", get_order_management_keyboard())
                            except Exception as ex:
                                print("Ошибка при обновлении заказа:", ex)
                                send_message(user_id, "Произошла ошибка при обновлении заказа. Пожалуйста, попробуйте позже.", get_order_management_keyboard())
                            finally:
                                conn.close()
                        else:
                            send_message(user_id, "Заказ не найден. Пожалуйста, создайте новый заказ.", get_main_keyboard())

                    elif state == 'return_order':
                        # Регистрируем возврат заказа
                        order = get_latest_order(user_id)
                        if order:
                            order_id = order['order_id']
                            try:
                                conn = get_db_connection()
                                cursor = conn.cursor()
                                cursor.execute(
                                    "UPDATE dbo.orders SET status = 'return_requested', updated_at = GETDATE() WHERE id = ?",
                                    (order_id,)
                                )
                                conn.commit()
                                
                                # Сохраняем причину возврата
                                add_order_note(order_id, f"Причина возврата: {message_text_full}")

                                send_message(user_id, "Запрос на возврат зарегистрирован. С вами свяжется администратор.", get_order_management_keyboard())
                            except Exception as ex:
                                print("Ошибка при регистрации возврата:", ex)
                                send_message(user_id, "Произошла ошибка при регистрации возврата. Пожалуйста, попробуйте позже.", get_order_management_keyboard())
                            finally:
                                conn.close()
                        else:
                            send_message(user_id, "Заказ не найден. Пожалуйста, создайте новый заказ.", get_main_keyboard())

                    # Возвращаем пользователя в исходное состояние
                    update_user_state(user_id, 'order_confirmed')
                    user_states[user_id]['state'] = 'order_confirmed'
                return
        # ...

    # 2. Если пользователь вне специальных состояний
    if contains_market_product_link(message_text_full) or contains_market_product_attachment(event):
        update_user_state(user_id, 'ordering')
        user_states[user_id] = {'state': 'ordering'}
        send_message(user_id,
                     ("Мы обнаружили товарную ссылку.\n"
                      "Хотите оформить заказ? Введите название, количество, контакт и адрес.\n"
                      "Для отмены введите \"Отмена\"."),
                     get_order_keyboard())
        return

    # 3. Основные команды
    if message_text == "оформить заказ":
        update_user_state(user_id, 'ordering')
        user_states[user_id] = {'state': 'ordering'}
        send_message(user_id,
                     "Введите данные для заказа (товар, кол-во, контакт, адрес). Или \"Отмена\".",
                     get_order_keyboard())
    elif message_text == "инфо о заказе":
        order = get_latest_order(user_id)
        if order:
            send_message(user_id,
                         f"Информация о заказе (ID {order['order_id']}):\n{order['order_data']}\nСтатус: {order['status']}",
                         get_order_management_keyboard())
        else:
            send_message(user_id,
                         "Заказ пока не оформлен. Нажмите \"Оформить заказ\".",
                         get_main_keyboard())
    elif message_text == "связаться с админом":
        update_user_state(user_id, 'admin_dialog')
        user_states[user_id] = {'state': 'admin_dialog'}
        send_message(user_id,
                     "Напишите сообщение администратору. Для выхода \"Вернуться в меню\".",
                     get_admin_keyboard())
    elif message_text == "помощь":
        send_message(user_id,
                     ("Меню:\n"
                      "1. Оформить заказ\n"
                      "2. Инфо о заказе\n"
                      "3. Связаться с админом\n"
                      "Или отправьте ссылку с товаром.\n"
                      "Состояние текущего заказа — \"Смена данных/размера\", \"Нет, отправил >2х мес\", \"Возврат\"."),
                     get_main_keyboard())
    else:
        send_message(user_id,
                     "Введите команду или отправьте ссылку на товар. Или попробуйте \"Помощь\".",
                     get_main_keyboard())

def main():
    print("Бот VK запущен...")

    try:
        for event in longpoll.listen():
            if event.type == VkEventType.MESSAGE_NEW and event.to_me:
                process_user_message(event)
    except KeyboardInterrupt:
        print("Бот остановлен пользователем.")
    except Exception as e:
        print("Ошибка:", e)
        time.sleep(1)

if __name__ == '__main__':
    main()
