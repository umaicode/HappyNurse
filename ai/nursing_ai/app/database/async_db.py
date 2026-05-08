from sqlalchemy.ext.asyncio import AsyncSession, async_sessionmaker, create_async_engine
from app.services.handover.config import Settings


def make_session_factory(settings: Settings):
    url = settings.database_url
    # psycopg2(sync) → asyncpg(async) 드라이버로 변환
    if url.startswith("postgresql://"):
        url = url.replace("postgresql://", "postgresql+asyncpg://", 1)
    elif url.startswith("postgres://"):
        url = url.replace("postgres://", "postgresql+asyncpg://", 1)
    engine = create_async_engine(url, pool_pre_ping=True)
    return async_sessionmaker(engine, class_=AsyncSession, expire_on_commit=False)