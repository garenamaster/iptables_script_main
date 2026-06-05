# Доступ к SecureGate через nginx + Basic Auth

```text
Браузер  --HTTP(S)+auth-->  nginx  --локально-->  Spring 127.0.0.1:8080
```

## Ошибка `options-ssl-nginx.conf: No such file`

Она появляется, если в конфиге указан SSL **до** запуска `certbot`.  
Сначала используйте **`securegate.conf`** (только порт 80), HTTPS — позже через **`securegate-ssl.conf`**.

---

## 1. Java на localhost

```properties
server.address=127.0.0.1
server.port=8080
```

```bash
curl -s http://127.0.0.1:8080/health
```

## 2. nginx + пароль

```bash
sudo apt install -y nginx apache2-utils
sudo htpasswd -c /etc/nginx/.htpasswd-securegate admin
```

## Видите «Welcome to nginx!» вместо SecureGate

Nginx отдаёт **дефолтный сайт**, а не ваш конфиг. На сервере:

```bash
# 1. Отключить стандартную заглушку
sudo rm -f /etc/nginx/sites-enabled/default

# 2. Убедиться, что подключён securegate
sudo ln -sf /etc/nginx/sites-available/securegate /etc/nginx/sites-enabled/securegate

# 3. В securegate.conf должны быть default_server и proxy на 8080
sudo nginx -t && sudo systemctl reload nginx

# 4. Java должна работать
curl -s http://127.0.0.1:8080/health
```

После этого `http://IP_СЕРВЕРА/` должен запросить логин/пароль и показать SecureGate.

---

## 3. Первый запуск (HTTP, без Let's Encrypt)

```bash
sudo cp securegate.conf /etc/nginx/sites-available/securegate
sudo nano /etc/nginx/sites-available/securegate
# Замените your.domain.example на IP или домен

# Удалите старый сломанный конфиг, если был:
sudo rm -f /etc/nginx/sites-enabled/securgate /etc/nginx/sites-enabled/securegate-ssl

sudo ln -sf /etc/nginx/sites-available/securegate /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl reload nginx
```

Открыть: `http://ВАШ_IP_ИЛИ_ДОМЕН/` → логин/пароль.

Проверка:

```bash
curl -I http://your.domain.example/          # ожидается 401
curl -u admin:PASSWORD -I http://your.domain.example/   # ожидается 200
```

## 4. HTTPS (когда есть домен)

```bash
sudo apt install -y certbot python3-certbot-nginx

# Временно оставьте securegate.conf на :80 для прохождения проверки certbot
sudo certbot --nginx -d your.domain.example
```

Certbot создаст `/etc/letsencrypt/options-ssl-nginx.conf` и сертификаты.

Дальше один из вариантов:

**A) Certbot сам правит nginx** — часто достаточно, проверьте `sudo nginx -t`.

**B) Вручную** — подключите наш SSL-конфиг:

```bash
sudo cp securegate-ssl.conf /etc/nginx/sites-available/securegate-ssl
sudo nano /etc/nginx/sites-available/securegate-ssl
# your.domain.example + пути к сертификатам

sudo rm -f /etc/nginx/sites-enabled/securegate
sudo ln -sf /etc/nginx/sites-available/securegate-ssl /etc/nginx/sites-enabled/
sudo nginx -t && sudo systemctl reload nginx
```

Открыть: `https://your.domain.example/`

## 5. Firewall

Снаружи: **80** и/или **443**, **22**. Порт **8080** не открывать.

## Быстрое исправление уже установленного конфига на сервере

Если `nginx -t` падает на `options-ssl-nginx.conf`:

```bash
sudo nano /etc/nginx/sites-enabled/securgate
```

Закомментируйте или удалите блок `listen 443` целиком и используйте только `listen 80` (как в `securegate.conf` из репозитория), затем:

```bash
sudo nginx -t && sudo systemctl reload nginx
```
