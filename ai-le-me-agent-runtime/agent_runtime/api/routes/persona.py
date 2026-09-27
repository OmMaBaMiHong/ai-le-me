from fastapi import APIRouter, Depends

from agent_runtime.api.deps import get_persona_service
from agent_runtime.schemas.persona import PersonaReportRequest, PersonaReportResponse
from agent_runtime.services.persona import PersonaReportService

router = APIRouter()


@router.post("/report/generate", response_model=PersonaReportResponse)
def generate_persona_report(
    payload: PersonaReportRequest,
    service: PersonaReportService = Depends(get_persona_service),
) -> PersonaReportResponse:
    return service.generate(payload)
