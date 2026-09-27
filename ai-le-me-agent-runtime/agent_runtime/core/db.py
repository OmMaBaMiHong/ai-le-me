from functools import lru_cache

from sqlalchemy import create_engine

from agent_runtime.core.config import get_settings


@lru_cache(maxsize=1)
def get_engine():
    settings = get_settings()
    return create_engine(settings.sqlalchemy_url, pool_pre_ping=True)
