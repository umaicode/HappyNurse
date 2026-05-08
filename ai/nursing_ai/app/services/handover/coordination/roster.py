from typing import Protocol


class RosterRepository(Protocol):
    async def fetch_active_encounters(self, practitioner_id: str) -> list[str]: ...


class RosterService:
    def __init__(self, repo: RosterRepository):
        self._repo = repo

    async def list_for_practitioner(self, practitioner_id: str) -> list[str]:
        return await self._repo.fetch_active_encounters(practitioner_id)
