from agent_runtime.skills.base import SkillDefinition


BUILTIN_SKILLS = [
    SkillDefinition(
        code="voice_bridge",
        name="Voice Bridge",
        description="Reserved adapter for speech-to-text and text-to-speech companion flows.",
        capability_type="voice",
        requires_approval=True,
    ),
    SkillDefinition(
        code="relationship_radar",
        name="Relationship Radar",
        description="Analyzes relationship temperature, pacing risk, and next-step timing.",
        capability_type="analysis",
    ),
    SkillDefinition(
        code="moment_ghostwriter",
        name="Moment Ghostwriter",
        description="Drafts personalized moment copy and hook ideas for content publishing.",
        capability_type="content",
        requires_approval=True,
    ),
]
