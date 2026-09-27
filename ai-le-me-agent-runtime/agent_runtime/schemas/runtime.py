from __future__ import annotations

from typing import Optional

from pydantic import BaseModel, Field


class RuntimeResolvedRoute(BaseModel):
    service_type: str
    provider_code: str
    profile_code: Optional[str] = None
    route_rule_id: Optional[int] = None
    configs: dict[str, str] = Field(default_factory=dict)


class RuntimeConfigSnapshot(BaseModel):
    ai_route: RuntimeResolvedRoute
    persona_provider: str
    companion_provider: str


class SkillDescriptor(BaseModel):
    code: str
    name: str
    description: str
    capability_type: str
    enabled_by_default: bool = True
    requires_approval: bool = False


class SkillCatalogResponse(BaseModel):
    skills: list[SkillDescriptor]


class IntegrationDescriptor(BaseModel):
    code: str
    name: str
    role: str
    selected: bool = False
    enabled: bool = False
    installed: bool = False
    package_name: Optional[str] = None
    docs_url: Optional[str] = None
    notes: list[str] = Field(default_factory=list)


class RuntimeLayerDescriptor(BaseModel):
    code: str
    name: str
    responsibility: str
    current_state: str
    next_builds: list[str] = Field(default_factory=list)


class RuntimePlaneDescriptor(BaseModel):
    code: str
    name: str
    responsibility: str
    modules: list[str] = Field(default_factory=list)
    notes: list[str] = Field(default_factory=list)


class RuntimeModuleDescriptor(BaseModel):
    code: str
    name: str
    role: str
    owner_boundary: str
    maturity: str
    notes: list[str] = Field(default_factory=list)


class CoreProductDescriptor(BaseModel):
    code: str
    name: str
    target_outcome: str
    monetization_anchor: str
    current_state: str
    required_layers: list[str] = Field(default_factory=list)


class PolicyCheckDescriptor(BaseModel):
    code: str
    name: str
    description: str
    required: bool = True


class PolicySurfaceDescriptor(BaseModel):
    code: str
    name: str
    checks: list[PolicyCheckDescriptor] = Field(default_factory=list)
    notes: list[str] = Field(default_factory=list)


class EvalMetricDescriptor(BaseModel):
    code: str
    name: str
    objective: str
    data_sources: list[str] = Field(default_factory=list)


class RuntimeArchitectureSnapshot(BaseModel):
    strategy_name: str
    north_star: str
    positioning: str
    java_core: str
    workflow: IntegrationDescriptor
    memory: list[IntegrationDescriptor]
    graph: list[IntegrationDescriptor]
    skills: IntegrationDescriptor
    runtime_spine: list[RuntimeLayerDescriptor] = Field(default_factory=list)
    product_planes: list[RuntimePlaneDescriptor] = Field(default_factory=list)
    platform_modules: list[RuntimeModuleDescriptor] = Field(default_factory=list)
    core_products: list[CoreProductDescriptor] = Field(default_factory=list)
    policy_surfaces: list[PolicySurfaceDescriptor] = Field(default_factory=list)
    eval_metrics: list[EvalMetricDescriptor] = Field(default_factory=list)
    rollout_order: list[str] = Field(default_factory=list)
    docs_path: str
