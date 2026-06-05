# Initializer Usage Guide

Модульный Python-инициализатор для развёртывания и сопровождения `projectipt` на сервере.

## 1) Что делает инициализатор

- проверяет окружение сервера (`systemd`, `iptables`, `java`, наличие JAR)
- ставит и запускает Java API как `systemd`-сервис
- ставит `systemd timer` для периодического reconcile
- проверяет работоспособность API (`/health`)
- применяет базовую безопасную конфигурацию firewall
- управляет временными разрешениями (IP/port с TTL)

## 2) Требования

- Linux-сервер с `systemd`
- `python3`
- `iptables`
- Java (по умолчанию `/usr/bin/java`)
- собранный JAR проекта (по умолчанию ожидается `/opt/projectipt/projectipt.jar`)
- права пользователя для `systemctl` и `iptables` (обычно root)

## 3) Быстрый старт (рекомендуемый порядок)

Запускать команды из корня проекта:

```bash
python3 -m initializer.cli <command> [options]
```

### Шаг 1. Bootstrap

```bash
python3 -m initializer.cli bootstrap \
  --jar-path /opt/projectipt/projectipt.jar \
  --workdir /opt/projectipt \
  --ssh-port 22
```

Что делает `bootstrap`:

1. валидирует окружение;
2. создаёт systemd units;
3. делает `systemctl daemon-reload`;
4. включает и запускает:
   - `projectipt-api.service`
   - `projectipt-reconcile.timer`
5. проверяет `/health`;
6. применяет базовый firewall (`setup-basic`), если не указан `--skip-setup-basic`.

> Если SSH на нестандартном порту, обязательно укажи правильный `--ssh-port`, иначе можно потерять удаленный доступ.

### Шаг 2. Проверка API

```bash
python3 -m initializer.cli healthcheck
```

### Шаг 3. Проверка таймера reconcile

```bash
systemctl status projectipt-reconcile.timer
```

## 4) Основные команды

### `bootstrap`

Подготовка сервера и запуск инфраструктуры.

Пример:

```bash
python3 -m initializer.cli bootstrap \
  --jar-path /opt/projectipt/projectipt.jar \
  --workdir /opt/projectipt \
  --java-exec /usr/bin/java \
  --service-user root \
  --service-group root \
  --reconcile-interval-min 2
```

### `healthcheck`

Проверка доступности API.

```bash
python3 -m initializer.cli healthcheck
```

### `reconcile`

Один цикл обслуживания:

- проверяет, что API-сервис активен;
- при падении выполняет перезапуск;
- повторно проверяет health;
- удаляет просроченные временные разрешения.

```bash
python3 -m initializer.cli reconcile
```

### `grant-temp`

Выдать временный доступ IP к порту.

```bash
python3 -m initializer.cli grant-temp \
  --ip 203.0.113.10 \
  --port 22 \
  --ttl-sec 1800 \
  --reason "temporary support access"
```

### `revoke-expired`

Принудительно удалить просроченные временные доступы.

```bash
python3 -m initializer.cli revoke-expired
```

## 5) Где хранится состояние

По умолчанию временные разрешения хранятся в:

- `/var/lib/projectipt-initializer/state.json`

Переопределить можно через `--state-file`.

## 6) Какие systemd units создаются

- `projectipt-api.service` — Java API
- `projectipt-reconcile.service` — одиночный reconcile run
- `projectipt-reconcile.timer` — периодический запуск reconcile

## 7) Полезные команды диагностики

```bash
systemctl status projectipt-api.service
systemctl status projectipt-reconcile.timer
systemctl list-timers | grep projectipt
journalctl -u projectipt-api.service -n 100 --no-pager
```

## 8) Частые проблемы

- `JAR not found`  
  Проверь путь `--jar-path`.

- `Required binary not found`  
  Установи отсутствующий бинарь (`systemctl`, `iptables`, `python3`, `java`).

- Ошибка после `setup-basic`  
  Проверь, что передан правильный `--ssh-port`.

