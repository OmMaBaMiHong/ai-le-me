from __future__ import annotations

from dataclasses import dataclass, field
from importlib import metadata, util


def _has_distribution(package_name: str | None) -> bool:
    if not package_name:
        return False
    try:
        metadata.version(package_name)
        return True
    except metadata.PackageNotFoundError:
        return False


def _has_module(module_name: str | None) -> bool:
    if not module_name:
        return False
    return util.find_spec(module_name) is not None


@dataclass(frozen=True)
class IntegrationProfile:
    code: str
    name: str
    role: str
    selected: bool = False
    enabled: bool = False
    package_name: str | None = None
    module_name: str | None = None
    docs_url: str | None = None
    notes: tuple[str, ...] = field(default_factory=tuple)

    @property
    def installed(self) -> bool:
        return _has_distribution(self.package_name) or _has_module(self.module_name)
