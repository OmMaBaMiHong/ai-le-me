from fastapi import APIRouter, Depends

from agent_runtime.api.deps import get_smart_match_service
from agent_runtime.schemas.match import SmartMatchRequest, SmartMatchResponse
from agent_runtime.services.matchmaker import SmartMatchService

router = APIRouter()


@router.post("/recommend", response_model=SmartMatchResponse)
def recommend_matches(
    payload: SmartMatchRequest,
    service: SmartMatchService = Depends(get_smart_match_service),
) -> SmartMatchResponse:
    return service.recommend(payload)


@router.post("/pregenerate", response_model=SmartMatchResponse)
def pregenerate_matches(
    payload: SmartMatchRequest,
    service: SmartMatchService = Depends(get_smart_match_service),
) -> SmartMatchResponse:
    return service.recommend(payload)
