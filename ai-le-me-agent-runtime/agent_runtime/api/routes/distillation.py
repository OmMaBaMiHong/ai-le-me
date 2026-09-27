from fastapi import APIRouter, Depends

from agent_runtime.api.deps import get_distillation_service
from agent_runtime.schemas.distillation import DistillationGenerateRequest, DistillationGenerateResponse
from agent_runtime.services.distillation import DistillationService

router = APIRouter()


@router.post("/profile/generate", response_model=DistillationGenerateResponse)
def generate_distillation_profile(
    payload: DistillationGenerateRequest,
    service: DistillationService = Depends(get_distillation_service),
) -> DistillationGenerateResponse:
    return service.generate(payload)
