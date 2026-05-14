from collections import defaultdict
from threading import Lock


class UnmatchedQueue:
    def __init__(self, max_contexts_per_token: int = 5):
        self._lock = Lock()
        self._counts: dict[str, int] = defaultdict(int)
        self._contexts: dict[str, list[str]] = defaultdict(list)
        self._max_ctx = max_contexts_per_token

    def record(self, token: str, context: str):
        with self._lock:
            self._counts[token] += 1
            ctxs = self._contexts[token]
            if len(ctxs) < self._max_ctx:
                ctxs.append(context)

    def snapshot(self):
        with self._lock:
            return [
                {"token": t, "count": c, "sample_contexts": list(self._contexts[t])}
                for t, c in self._counts.items()
            ]
