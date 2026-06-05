from __future__ import annotations

import json
import time
from dataclasses import dataclass
from pathlib import Path


@dataclass
class TemporaryRule:
    ip: str
    port: int
    expires_at: int
    reason: str

    def to_dict(self) -> dict:
        return {
            "ip": self.ip,
            "port": self.port,
            "expires_at": self.expires_at,
            "reason": self.reason,
        }

    @staticmethod
    def from_dict(data: dict) -> "TemporaryRule":
        return TemporaryRule(
            ip=str(data["ip"]),
            port=int(data["port"]),
            expires_at=int(data["expires_at"]),
            reason=str(data.get("reason", "")),
        )


def load_rules(path: Path) -> list[TemporaryRule]:
    if not path.exists():
        return []
    raw = json.loads(path.read_text(encoding="utf-8"))
    return [TemporaryRule.from_dict(item) for item in raw]


def save_rules(path: Path, rules: list[TemporaryRule]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    payload = [rule.to_dict() for rule in rules]
    path.write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")


def split_expired(rules: list[TemporaryRule]) -> tuple[list[TemporaryRule], list[TemporaryRule]]:
    now = int(time.time())
    expired = [r for r in rules if r.expires_at <= now]
    active = [r for r in rules if r.expires_at > now]
    return active, expired

