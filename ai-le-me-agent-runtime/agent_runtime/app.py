from fastapi import FastAPI

from agent_runtime.api.routes import companion, distillation, health, match, persona, runtime
from agent_runtime.core.config import get_settings


def create_app() -> FastAPI:
    settings = get_settings()
    app = FastAPI(
        title=settings.app_name,
        version="0.1.0",
        docs_url="/docs",
        redoc_url="/redoc",
    )
    app.include_router(health.router)
    app.include_router(runtime.router, prefix="/runtime", tags=["runtime"])
    app.include_router(persona.router, prefix="/persona", tags=["persona"])
    app.include_router(distillation.router, prefix="/distillation", tags=["distillation"])
    app.include_router(companion.router, prefix="/companion", tags=["companion"])
    app.include_router(match.router, prefix="/match", tags=["match"])
    return app
