from fastapi import APIRouter

from agent_runtime.core.config import get_settings

router = APIRouter(tags=["health"])


@router.get("/health")
def health_check() -> dict[str, object]:
    settings = get_settings()
    return {
        "ok": True,
        "service": settings.app_name,
        "env": settings.app_env,
        "sharedConfigEnabled": settings.mysql_read_shared_config,
        "runtimeTablesEnabled": settings.mysql_enable_runtime_tables,
    }
