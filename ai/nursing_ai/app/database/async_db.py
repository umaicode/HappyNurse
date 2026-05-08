from sqlalchemy.ext.asyncio import AsyncSession, async_sessionmaker, create_async_engine
from app.services.handover.config import Settings


def make_session_factory(settings: Settings):
    engine = create_async_engine(settings.database_url, pool_pre_ping=True)
    return async_sessionmaker(engine, class_=AsyncSession, expire_on_commit=False)