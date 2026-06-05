from __future__ import annotations

import argparse
import time
from pathlib import Path

from .checks import validate_host
from .config import InitConfig
from .firewall_api import ApiClient
from .reconcile import reconcile, run_healthcheck
from .state import TemporaryRule, load_rules, save_rules, split_expired
from .systemd_manager import install_units


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description="projectipt host initializer")
    parser.add_argument("--api-base-url", default="http://127.0.0.1:8080")
    parser.add_argument("--jar-path", default="/opt/projectipt/projectipt.jar")
    parser.add_argument("--workdir", default="/opt/projectipt")
    parser.add_argument("--java-exec", default="/usr/bin/java")
    parser.add_argument("--service-user", default="root")
    parser.add_argument("--service-group", default="root")
    parser.add_argument("--reconcile-interval-min", type=int, default=2)
    parser.add_argument("--state-file", default="/var/lib/projectipt-initializer/state.json")

    sub = parser.add_subparsers(dest="command", required=True)

    b = sub.add_parser("bootstrap", help="prepare host, install systemd units, setup firewall")
    b.add_argument("--ssh-port", type=int, default=22)
    b.add_argument("--skip-setup-basic", action="store_true")

    sub.add_parser("healthcheck", help="run API health check")
    sub.add_parser("reconcile", help="restart-on-failure and revoke expired temp rules")

    gt = sub.add_parser("grant-temp", help="grant temporary IP->port access")
    gt.add_argument("--ip", required=True)
    gt.add_argument("--port", required=True, type=int)
    gt.add_argument("--ttl-sec", required=True, type=int)
    gt.add_argument("--reason", default="")

    sub.add_parser("revoke-expired", help="remove expired temporary rules now")
    return parser


def to_config(args: argparse.Namespace) -> InitConfig:
    return InitConfig(
        api_base_url=args.api_base_url,
        jar_path=Path(args.jar_path),
        workdir=Path(args.workdir),
        java_exec=args.java_exec,
        user=args.service_user,
        group=args.service_group,
        reconcile_interval_min=args.reconcile_interval_min,
        state_file=Path(args.state_file),
    )


def cmd_bootstrap(config: InitConfig, ssh_port: int, skip_setup_basic: bool) -> None:
    validate_host(config)
    install_units(config, Path(__file__).resolve())
    run_healthcheck(config)
    if not skip_setup_basic:
        ApiClient(config.api_base_url).setup_basic(ssh_port)


def cmd_grant_temp(config: InitConfig, ip: str, port: int, ttl_sec: int, reason: str) -> None:
    expires_at = int(time.time()) + ttl_sec
    api = ApiClient(config.api_base_url)
    api.allow_temp(ip, port)

    rules = load_rules(config.state_file)
    # Upsert by (ip, port)
    rules = [r for r in rules if not (r.ip == ip and r.port == port)]
    rules.append(TemporaryRule(ip=ip, port=port, expires_at=expires_at, reason=reason))
    save_rules(config.state_file, rules)


def cmd_revoke_expired(config: InitConfig) -> None:
    api = ApiClient(config.api_base_url)
    rules = load_rules(config.state_file)
    active, expired = split_expired(rules)
    for rule in expired:
        api.revoke_temp(rule.ip, rule.port)
    save_rules(config.state_file, active)


def main() -> None:
    parser = build_parser()
    args = parser.parse_args()
    config = to_config(args)

    if args.command == "bootstrap":
        cmd_bootstrap(config, ssh_port=args.ssh_port, skip_setup_basic=args.skip_setup_basic)
    elif args.command == "healthcheck":
        run_healthcheck(config)
    elif args.command == "reconcile":
        reconcile(config)
    elif args.command == "grant-temp":
        cmd_grant_temp(config, ip=args.ip, port=args.port, ttl_sec=args.ttl_sec, reason=args.reason)
    elif args.command == "revoke-expired":
        cmd_revoke_expired(config)
    else:
        parser.error("Unknown command")


if __name__ == "__main__":
    main()

