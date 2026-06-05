from __future__ import annotations

import json
from dataclasses import dataclass
from urllib.error import HTTPError, URLError
from urllib.parse import urlencode
from urllib.request import Request, urlopen


@dataclass
class ApiClient:
    base_url: str

    def _request(self, method: str, path: str, params: dict[str, str | int] | None = None) -> dict:
        query = f"?{urlencode(params)}" if params else ""
        url = f"{self.base_url}{path}{query}"
        req = Request(url, method=method)
        try:
            with urlopen(req, timeout=5) as resp:
                body = resp.read().decode("utf-8")
                return json.loads(body)
        except HTTPError as e:
            body = e.read().decode("utf-8", errors="ignore")
            raise RuntimeError(f"HTTP {e.code} calling {url}: {body}")
        except URLError as e:
            raise RuntimeError(f"API unavailable {url}: {e.reason}")

    def health(self) -> dict:
        return self._request("GET", "/health")

    def setup_basic(self, ssh_port: int) -> dict:
        return self._request("POST", "/api/firewall/setup-basic", {"sshPort": ssh_port})

    def allow_temp(self, ip: str, port: int) -> dict:
        return self._request("POST", "/api/firewall/allow-port", {"ip": ip, "port": port})

    def revoke_temp(self, ip: str, port: int) -> dict:
        return self._request("DELETE", "/api/firewall/allow-port", {"ip": ip, "port": port})

