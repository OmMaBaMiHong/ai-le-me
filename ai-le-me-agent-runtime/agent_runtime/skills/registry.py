from agent_runtime.schemas.runtime import SkillDescriptor
from agent_runtime.skills.base import SkillDefinition
from agent_runtime.skills.builtin import BUILTIN_SKILLS


class SkillRegistry:
    def __init__(self, skills: list[SkillDefinition]) -> None:
        self._skills = {skill.code: skill for skill in skills}

    def list_skills(self) -> list[SkillDescriptor]:
        return [
            SkillDescriptor(
                code=skill.code,
                name=skill.name,
                description=skill.description,
                capability_type=skill.capability_type,
                enabled_by_default=skill.enabled_by_default,
                requires_approval=skill.requires_approval,
            )
            for skill in self._skills.values()
        ]


def build_default_skill_registry() -> SkillRegistry:
    return SkillRegistry(BUILTIN_SKILLS)
