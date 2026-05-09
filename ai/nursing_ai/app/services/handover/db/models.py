from sqlalchemy import BigInteger, Column, DateTime, Text, func
from sqlalchemy.dialects.postgresql import JSONB
from sqlalchemy.orm import DeclarativeBase


class Base(DeclarativeBase):
    pass


class ShiftHandover(Base):
    __tablename__ = "shift_handover"

    handover_id          = Column(BigInteger, primary_key=True, autoincrement=True)
    encounter_id         = Column(BigInteger, nullable=False, index=True)
    from_practitioner_id = Column(BigInteger, nullable=False, index=True)
    auto_summary         = Column(Text, nullable=False)
    auto_summary_json    = Column(JSONB, nullable=True)
    created_at           = Column(DateTime(timezone=False), server_default=func.now(), nullable=False)