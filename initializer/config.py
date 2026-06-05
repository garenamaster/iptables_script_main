from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path


@dataclass
class InitConfig:
    api_base_url: str = "http://127.0.0.1:8080"
    service_name: str = "projectipt-api.service"
    reconcile_service_name: str = "projectipt-reconcile.service"
    reconcile_timer_name: str = "projectipt-reconcile.timer"
    state_file: Path = Path("/var/lib/projectipt-initializer/state.json")
    java_exec: str = "/usr/bin/java"
    jar_path: Path = Path("/opt/projectipt/projectipt.jar")
    workdir: Path = Path("/opt/projectipt")
    user: str = "root"
    group: str = "root"
    reconcile_interval_min: int = 2

