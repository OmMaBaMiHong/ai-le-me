from __future__ import annotations

from typing import Optional

from pydantic import BaseModel, Field

from agent_runtime.schemas.common import UserCard


class MatchPreference(BaseModel):
    preferred_cities: list[str] = Field(default_factory=list)
    age_min: Optional[int] = None
    age_max: Optional[int] = None
    height_min: Optional[int] = None
    height_max: Optional[int] = None
    education_levels: list[int] = Field(default_factory=list)
    prefer_fans: bool = False
    prefer_liked: bool = False
    only_liked: bool = False


class MatchCandidate(BaseModel):
    user_id: int
    nickname: Optional[str] = None
    gender: Optional[str] = None
    city: Optional[str] = None
    age: Optional[int] = None
    summary: Optional[str] = None
    tags: list[str] = Field(default_factory=list)
    height: Optional[int] = None
    education: Optional[int] = None
    job: Optional[str] = None
    interests: list[str] = Field(default_factory=list)
    vip: bool = False
    verified: bool = False
    identity_verified: bool = False
    education_verified: bool = False
    completeness: int = Field(default=0, ge=0, le=100)
    last_active_hours: Optional[int] = None
    liked_by_owner: bool = False
    likes_owner: bool = False


class SmartMatchRequest(BaseModel):
    owner: UserCard
    preference: MatchPreference = Field(default_factory=MatchPreference)
    candidates: list[MatchCandidate] = Field(default_factory=list)
    limit: int = Field(default=6, ge=1, le=100)


class SmartMatchItem(BaseModel):
    user_id: int
    score: int = Field(ge=0, le=100)
    summary: str
    fit_tags: list[str] = Field(default_factory=list)
    reasons: list[str] = Field(default_factory=list)
    icebreak_openers: list[str] = Field(default_factory=list)
    recommended_action: str = Field(default="open_profile")
    debug_trace_id: Optional[str] = None


class SmartMatchResponse(BaseModel):
    provider: str
    route_profile: Optional[str] = None
    insight: str
    matches: list[SmartMatchItem] = Field(default_factory=list)
    debug_trace_id: Optional[str] = None
    retrieval_hits: list[str] = Field(default_factory=list)
    graph_facts: list[str] = Field(default_factory=list)
    reasoning_summary: Optional[str] = None
