from __future__ import annotations

from pathlib import Path

from .config import InitConfig
from .utils import require_binary, run


def validate_host(config: InitConfig) -> None:
    require_binary("systemctl")
    require_binary("iptables")
    require_binary("python3")
    require_binary(Path(config.java_exec).name)

    if not config.jar_path.exists():
        raise RuntimeError(f"JAR not found: {config.jar_path}")

    ssh = run(["ss", "-tln"], check=False)
    if "LISTEN" not in ssh.out:
        raise RuntimeError("Cannot detect listening sockets (ss -tln)")

