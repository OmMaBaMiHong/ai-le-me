from __future__ import annotations

from typing import Optional, Union

from pydantic import BaseModel, Field


class UserCard(BaseModel):
    user_id: Optional[int] = Field(default=None)
    nickname: Optional[str] = Field(default=None)
    gender: Optional[str] = Field(default=None)
    city: Optional[str] = Field(default=None)
    age: Optional[int] = Field(default=None)
    summary: Optional[str] = Field(default=None)
    tags: list[str] = Field(default_factory=list)


class MessageTurn(BaseModel):
    role: str = Field(description="owner / target / system")
    content: str
    timestamp: Optional[str] = None


class SignalItem(BaseModel):
    name: str
    value: Union[float, int, str, bool]
    note: Optional[str] = None


class MomentItem(BaseModel):
    moment_id: Optional[int] = None
    text: Optional[str] = None
    tags: list[str] = Field(default_factory=list)
    media_type: Optional[str] = None


class TraitScore(BaseModel):
    name: str
    score: int = Field(ge=0, le=100)
    reason: str
