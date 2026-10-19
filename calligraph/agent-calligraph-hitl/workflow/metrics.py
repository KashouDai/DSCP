from dataclasses import dataclass


@dataclass
class RolloutMetrics:
    total: int = 0
    passed: int = 0
    failed: int = 0
    fallback: int = 0
    elapsed_ms: float = 0

    def record(self, report, elapsed_ms: float = 0):
        self.total += 1
        self.elapsed_ms += elapsed_ms
        if report.passed:
            self.passed += 1
        else:
            self.failed += 1
        if report.fallback:
            self.fallback += 1

    @property
    def failure_rate(self) -> float:
        return self.failed / self.total if self.total else 0.0

    @property
    def average_elapsed_ms(self) -> float:
        return self.elapsed_ms / self.total if self.total else 0.0

    def should_rollback(self, threshold: float = 0.2) -> bool:
        return self.total > 0 and self.failure_rate > threshold

    def snapshot(self) -> dict:
        return {
            "total": self.total,
            "passed": self.passed,
            "failed": self.failed,
            "fallback": self.fallback,
            "failure_rate": self.failure_rate,
            "average_elapsed_ms": self.average_elapsed_ms,
        }
