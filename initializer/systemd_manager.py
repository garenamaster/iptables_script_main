from __future__ import annotations

from pathlib import Path

from .config import InitConfig
from .utils import run


SYSTEMD_DIR = Path("/etc/systemd/system")


def _service_unit(config: InitConfig) -> str:
    return f"""[Unit]
Description=projectipt Java API
After=network.target

[Service]
Type=simple
User={config.user}
Group={config.group}
WorkingDirectory={config.workdir}
ExecStart={config.java_exec} -jar {config.jar_path}
Restart=always
RestartSec=3

[Install]
WantedBy=multi-user.target
"""


def _reconcile_service(config: InitConfig, python_entry: Path) -> str:
    return f"""[Unit]
Description=projectipt reconcile loop
After=network.target {config.service_name}

[Service]
Type=oneshot
ExecStart=/usr/bin/python3 {python_entry} reconcile
"""


def _reconcile_timer(config: InitConfig) -> str:
    return f"""[Unit]
Description=Run projectipt reconcile periodically

[Timer]
OnBootSec=45s
OnUnitActiveSec={config.reconcile_interval_min}min
Unit={config.reconcile_service_name}

[Install]
WantedBy=timers.target
"""


def install_units(config: InitConfig, python_entry: Path) -> None:
    (SYSTEMD_DIR / config.service_name).write_text(_service_unit(config), encoding="utf-8")
    (SYSTEMD_DIR / config.reconcile_service_name).write_text(
        _reconcile_service(config, python_entry), encoding="utf-8"
    )
    (SYSTEMD_DIR / config.reconcile_timer_name).write_text(_reconcile_timer(config), encoding="utf-8")

    run(["systemctl", "daemon-reload"])
    run(["systemctl", "enable", "--now", config.service_name])
    run(["systemctl", "enable", "--now", config.reconcile_timer_name])


def restart_api(config: InitConfig) -> None:
    run(["systemctl", "restart", config.service_name])


def is_active(unit: str) -> bool:
    return run(["systemctl", "is-active", unit], check=False).ok

