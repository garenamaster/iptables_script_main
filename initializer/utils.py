from __future__ import annotations

import shutil
import subprocess
from dataclasses import dataclass
from typing import Sequence


@dataclass
class CmdResult:
    code: int
    out: str
    err: str

    @property
    def ok(self) -> bool:
        return self.code == 0


def require_binary(name: str) -> None:
    if shutil.which(name) is None:
        raise RuntimeError(f"Required binary not found: {name}")


def run(cmd: Sequence[str], check: bool = True) -> CmdResult:
    proc = subprocess.run(cmd, capture_output=True, text=True)
    result = CmdResult(proc.returncode, proc.stdout.strip(), proc.stderr.strip())
    if check and not result.ok:
        details = result.err or result.out or "unknown error"
        raise RuntimeError(f"Command failed: {' '.join(cmd)} :: {details}")
    return result

