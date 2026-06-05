from __future__ import annotations

from .config import InitConfig
from .firewall_api import ApiClient
from .state import load_rules, save_rules, split_expired
from .systemd_manager import is_active, restart_api


def run_healthcheck(config: InitConfig) -> None:
    api = ApiClient(config.api_base_url)
    payload = api.health()
    if not payload.get("success", False):
        raise RuntimeError(f"Health check failed: {payload}")


def reconcile(config: InitConfig) -> None:
    if not is_active(config.service_name):
        restart_api(config)

    try:
        run_healthcheck(config)
    except Exception:
        restart_api(config)
        run_healthcheck(config)

    api = ApiClient(config.api_base_url)
    rules = load_rules(config.state_file)
    active, expired = split_expired(rules)
    for rule in expired:
        try:
            api.revoke_temp(rule.ip, rule.port)
        except Exception:
            # Keep reconcile resilient: next run retries cleanup.
            active.append(rule)
    save_rules(config.state_file, active)

