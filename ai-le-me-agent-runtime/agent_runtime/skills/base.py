from dataclasses import dataclass


@dataclass(frozen=True)
class SkillDefinition:
    code: str
    name: str
    description: str
    capability_type: str
    enabled_by_default: bool = True
    requires_approval: bool = False
