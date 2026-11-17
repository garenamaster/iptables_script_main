# projectipt

REST API для управления `iptables` на локальном сервере.

По умолчанию приложение слушает `127.0.0.1:8080`.

Веб-интерфейс (SecureGate) на сервере:

- **локально на сервере:** `http://127.0.0.1:8080/`
- **с удалённого ПК (рекомендуется):** `https://ваш-домен/` через **nginx + Basic Auth**

Подробная инструкция: [deploy/nginx/README.md](deploy/nginx/README.md)

Конфигурация дашборда в `application.properties`:

- `firewall.ssh-port=22`
- `firewall.blocked-ports=22,3306`

## Требования

- Linux с установленным `iptables` (`/usr/sbin/iptables`)
- Для `saveRules()` нужен `iptables-save` (`/usr/sbin/iptables-save`)
- Процесс приложения должен иметь права на управление firewall
```bash
sudo apt update

sudo apt install -y \
    openjdk-17-jdk \
    python3 \
    iptables \
    nginx \
    apache2-utils \
    git \
    curl
```

## Запуск

```bash
./mvnw spring-boot:run
```

Базовый URL:

```bash
BASE_URL="http://127.0.0.1:8080/api/firewall"
```

Проверка здоровья:

```bash
curl -s "http://127.0.0.1:8080/health"
```

## Формат ответа

Все ответы возвращаются в JSON:

```json
{
  "success": true,
  "message": "Порт открыт",
  "data": null
}
```

## Идемпотентность endpoint'ов

Для всех mutating endpoint'ов реализована идемпотентность: повтор одного и того же запроса не должен менять состояние firewall после первого успешного применения.

- `POST /allow` - повтор не создает дубликаты правил.
- `POST /allow-port` - повтор не создает дубликаты правил.
- `DELETE /allow-port` - повтор после удаления не меняет состояние.
- `POST /block` - повтор не создает дубликаты `DROP`.
- `DELETE /block` - повтор после разблокировки не меняет состояние.
- `POST /open-port` - повтор не создает дубликаты `ACCEPT`.
- `DELETE /open-port` - удаляет все дубликаты `ACCEPT` для порта; повтор не меняет состояние.
- `POST /set-policy` - повтор с теми же параметрами не меняет итоговую конфигурацию.
- `POST /close-all` - повтор сохраняет `INPUT DROP`.
- `POST /flush-chain` - повтор для пустой цепочки не меняет состояние.
- `POST /chain` - если цепочка уже существует, повтор является no-op.
- `DELETE /chain` - если цепочки нет, повтор является no-op.
- `POST /save` - возвращает актуальный snapshot правил, состояние firewall не изменяет.
- `POST /setup-basic` - повтор приводит систему к той же базовой конфигурации.
- `DELETE /rule-line` - повтор с теми же `chain` и `lineNumber` считается no-op в рамках текущего процесса API.

## Примеры curl

### Разрешить IP к стандартным портам (80, 443)

```bashss
curl -s -X POST "$BASE_URL/allow" \
  -d "ip=203.0.113.10"
```

### Разрешить IP к конкретному порту

```bash
curl -s -X POST "$BASE_URL/allow-port" \
  -d "ip=203.0.113.10" \
  -d "port=22"
```

### Удалить правило разрешения IP к порту

```bash
curl -s -X DELETE "$BASE_URL/allow-port?ip=203.0.113.10&port=22"
```

### Полностью заблокировать IP (DROP)

```bash
curl -s -X POST "$BASE_URL/block" \
  -d "ip=203.0.113.10"
```

### Снять блокировку IP

```bash
curl -s -X DELETE "$BASE_URL/block?ip=203.0.113.10"
```

### Открыть порт для всех (ACCEPT)

```bash
curl -s -X POST "$BASE_URL/open-port" \
  -d "port=8081"
```

### Закрыть порт (удалить правило ACCEPT)

```bash
curl -s -X DELETE "$BASE_URL/open-port?port=8081"
```

### Установить политику цепочки

```bash
curl -s -X POST "$BASE_URL/set-policy" \
  -d "chain=INPUT" \
  -d "policy=DROP"
```

### Закрыть все входящие соединения (INPUT DROP)

```bash
curl -s -X POST "$BASE_URL/close-all"
```

### Получить список правил

```bash
curl -s "$BASE_URL/rules"
```

### Получить подробный список правил (с номерами)

```bash
curl -s "$BASE_URL/rules-detailed"
```

### Проверить, существует ли правило

```bash
curl -s "$BASE_URL/rule-exists?ip=203.0.113.10&port=22"
```

### Удалить правило по номеру в цепочке

```bash
curl -s -X DELETE "$BASE_URL/rule-line?chain=INPUT&lineNumber=3"
```

### Очистить цепочку

```bash
curl -s -X POST "$BASE_URL/flush-chain" \
  -d "chain=INPUT"
```

### Создать цепочку

```bash
curl -s -X POST "$BASE_URL/chain" \
  -d "chainName=MY_CHAIN"
```

### Удалить цепочку

```bash
curl -s -X DELETE "$BASE_URL/chain?chainName=MY_CHAIN"
```

### Сохранить текущие правила

```bash
curl -s -X POST "$BASE_URL/save"
```

### Получить список открытых портов

```bash
curl -s "$BASE_URL/open-ports"
```

### Получить общий статус firewall

```bash
curl -s "$BASE_URL/status"
```

### Применить базовую конфигурацию firewall

```bash
curl -s -X POST "$BASE_URL/setup-basic?sshPort=22"
```

Что делает `setup-basic`:

- очищает цепочки `INPUT`, `FORWARD`, `OUTPUT`
- устанавливает политики:
  - `INPUT DROP`
  - `FORWARD DROP`
  - `OUTPUT ACCEPT`
- добавляет разрешения:
  - loopback (`lo`)
  - `ESTABLISHED,RELATED`
  - SSH на `sshPort`
  - HTTP (`80`)
  - HTTPS (`443`)

Важно: если SSH работает не на `22`, обязательно передай корректный `sshPort`, иначе можно потерять удаленный доступ к серверу.

## Полезно для отладки

Если хочется красивый JSON-вывод:

```bash
curl -s "$BASE_URL/status" | jq
```
