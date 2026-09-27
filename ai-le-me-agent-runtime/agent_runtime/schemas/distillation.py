from __future__ import annotations

from typing import Any, Literal, Optional

from pydantic import BaseModel, Field

from agent_runtime.schemas.common import UserCard


class DistillationAnswer(BaseModel):
    question_code: str
    question_label: Optional[str] = None
    answer_text: str


class DistillationMaterial(BaseModel):
    material_type: Literal["text_note", "chat_export_text", "chat_screenshot", "social_screenshot", "image_photo"]
    label: Optional[str] = None
    content: Optional[str] = None
    file_url: Optional[str] = None


class DistillationGenerateRequest(BaseModel):
    scene_type: Literal["self_bootstrap", "private_person_analysis"]
    subject_type: Literal["self", "private_person", "public_figure"] = "private_person"
    relation_label: Optional[str] = None
    analysis_goal: Optional[str] = None
    owner: Optional[UserCard] = None
    subject: UserCard
    answers: list[DistillationAnswer] = Field(default_factory=list)
    materials: list[DistillationMaterial] = Field(default_factory=list)
    signals: list[dict[str, Any]] = Field(default_factory=list)
    policy_context: dict[str, Any] = Field(default_factory=dict)


class DistillationEvidenceCard(BaseModel):
    kind: str
    title: str
    detail: str
    source_types: list[str] = Field(default_factory=list)


class DistillationServiceHooks(BaseModel):
    recommended_opening_style: str
    matchmaker_style_hint: str
    assistant_guardrails: list[str] = Field(default_factory=list)
    profile_copy_hint: str


class DistillationGenerateResponse(BaseModel):
    scene_type: str
    subject_name: str
    relation_label: Optional[str] = None
    summary: str
    core_insights: list[str] = Field(default_factory=list)
    interaction_guidance: list[str] = Field(default_factory=list)
    risk_flags: list[str] = Field(default_factory=list)
    evidence_cards: list[DistillationEvidenceCard] = Field(default_factory=list)
    confidence_notes: list[str] = Field(default_factory=list)
    persona_kernel: dict[str, Any] = Field(default_factory=dict)
    service_hooks: DistillationServiceHooks
