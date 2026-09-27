from functools import lru_cache

from pydantic import Field
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", env_file_encoding="utf-8", extra="ignore")

    app_name: str = Field(default="AiLeMe Agent Runtime", alias="APP_NAME")
    app_env: str = Field(default="dev", alias="APP_ENV")
    app_port: int = Field(default=8091, alias="APP_PORT")
    log_level: str = Field(default="INFO", alias="LOG_LEVEL")

    mysql_host: str = Field(default="127.0.0.1", alias="MYSQL_HOST")
    mysql_port: int = Field(default=3306, alias="MYSQL_PORT")
    mysql_database: str = Field(default="aileme", alias="MYSQL_DATABASE")
    mysql_user: str = Field(default="root", alias="MYSQL_USER")
    mysql_password: str = Field(default="", alias="MYSQL_PASSWORD")
    mysql_charset: str = Field(default="utf8mb4", alias="MYSQL_CHARSET")
    mysql_read_shared_config: bool = Field(default=True, alias="MYSQL_READ_SHARED_CONFIG")
    mysql_enable_runtime_tables: bool = Field(default=False, alias="MYSQL_ENABLE_RUNTIME_TABLES")

    third_party_provider_table: str = Field(default="sys_third_party_provider", alias="THIRD_PARTY_PROVIDER_TABLE")
    third_party_route_table: str = Field(default="sys_third_party_route_rule", alias="THIRD_PARTY_ROUTE_TABLE")

    default_ai_service_type: str = Field(default="ai", alias="DEFAULT_AI_SERVICE_TYPE")
    default_ai_provider: str = Field(default="openai_codex", alias="DEFAULT_AI_PROVIDER")
    default_persona_provider: str = Field(default="persona_v2", alias="DEFAULT_PERSONA_PROVIDER")
    default_companion_provider: str = Field(default="companion_v1", alias="DEFAULT_COMPANION_PROVIDER")
    llm_model_fallback: str = Field(default="", alias="LLM_MODEL_FALLBACK")
    llm_api_protocol: str = Field(default="chat_completions", alias="LLM_API_PROTOCOL")
    llm_request_timeout: int = Field(default=45, alias="LLM_REQUEST_TIMEOUT")
    llm_enable_debug_trace: bool = Field(default=False, alias="LLM_ENABLE_DEBUG_TRACE")
    llm_prefer_env_default: bool = Field(default=True, alias="LLM_PREFER_ENV_DEFAULT")
    llm_default_provider_code: str = Field(default="openai", alias="LLM_DEFAULT_PROVIDER_CODE")
    llm_default_profile_code: str = Field(default="gpt_5_4", alias="LLM_DEFAULT_PROFILE_CODE")
    llm_default_endpoint: str = Field(default="https://api.openai.com/v1", alias="LLM_DEFAULT_ENDPOINT")
    llm_default_api_key: str = Field(default="", alias="OPENAI_API_KEY")
    llm_default_model: str = Field(default="gpt-5.4", alias="OPENAI_MODEL")
    llm_default_api_protocol: str = Field(default="responses", alias="OPENAI_API_PROTOCOL")
    llm_default_reasoning_effort: str = Field(default="medium", alias="OPENAI_REASONING_EFFORT")
    llm_default_max_output_tokens: int = Field(default=1600, alias="OPENAI_MAX_OUTPUT_TOKENS")

    workflow_engine: str = Field(default="langgraph", alias="WORKFLOW_ENGINE")
    workflow_enabled: bool = Field(default=False, alias="WORKFLOW_ENABLED")
    memory_primary: str = Field(default="mem0", alias="MEMORY_PRIMARY")
    memory_secondary: str = Field(default="letta", alias="MEMORY_SECONDARY")
    graph_primary: str = Field(default="graphiti", alias="GRAPH_PRIMARY")
    graph_secondary: str = Field(default="neo4j", alias="GRAPH_SECONDARY")
    skills_reference: str = Field(default="openclaw", alias="SKILLS_REFERENCE")
    skills_adapter_mode: str = Field(default="controlled_adapter", alias="SKILLS_ADAPTER_MODE")
    local_memory_enabled: bool = Field(default=True, alias="LOCAL_MEMORY_ENABLED")
    local_graph_enabled: bool = Field(default=True, alias="LOCAL_GRAPH_ENABLED")
    local_vector_enabled: bool = Field(default=True, alias="LOCAL_VECTOR_ENABLED")
    retrieval_memory_fetch_multiplier: int = Field(default=3, alias="RETRIEVAL_MEMORY_FETCH_MULTIPLIER")
    retrieval_memory_score_threshold: float = Field(default=0.12, alias="RETRIEVAL_MEMORY_SCORE_THRESHOLD")
    retrieval_candidate_score_threshold: float = Field(default=0.1, alias="RETRIEVAL_CANDIDATE_SCORE_THRESHOLD")
    retrieval_hypothetical_enabled: bool = Field(default=True, alias="RETRIEVAL_HYPOTHETICAL_ENABLED")
    retrieval_hypothetical_per_text: int = Field(default=2, alias="RETRIEVAL_HYPOTHETICAL_PER_TEXT")
    ollama_base_url: str = Field(default="http://127.0.0.1:11434", alias="OLLAMA_BASE_URL")
    ollama_embed_model: str = Field(default="nomic-embed-text", alias="OLLAMA_EMBED_MODEL")
    chroma_host: str = Field(default="127.0.0.1", alias="CHROMA_HOST")
    chroma_port: int = Field(default=8008, alias="CHROMA_PORT")
    chroma_collection_prefix: str = Field(default="aileme_agent", alias="CHROMA_COLLECTION_PREFIX")
    neo4j_uri: str = Field(default="bolt://127.0.0.1:7687", alias="NEO4J_URI")
    neo4j_user: str = Field(default="neo4j", alias="NEO4J_USER")
    neo4j_password: str = Field(default="aileme-agent-dev", alias="NEO4J_PASSWORD")
    neo4j_database: str = Field(default="neo4j", alias="NEO4J_DATABASE")

    @property
    def sqlalchemy_url(self) -> str:
        password = self.mysql_password
        return (
            f"mysql+pymysql://{self.mysql_user}:{password}@{self.mysql_host}:{self.mysql_port}/"
            f"{self.mysql_database}?charset={self.mysql_charset}"
        )


@lru_cache(maxsize=1)
def get_settings() -> Settings:
    return Settings()
