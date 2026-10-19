from typing import Literal, Self

from pydantic import BaseModel, ConfigDict, Field, JsonValue, model_validator


class TaskDecision(BaseModel):
    model_config = ConfigDict(
        extra="forbid",
        strict=True,
        str_strip_whitespace=True,
    )

    is_single: bool = Field(
        description="用户请求是否只需要调用一个工具",
    )
    first_subtask: str | None = Field(
        default=None,
        min_length=1,
        description="多工具任务的第一个子任务；单工具任务必须为空",
    )

    @model_validator(mode="after")
    def validate_task_shape(self) -> Self:
        if self.is_single and self.first_subtask is not None:
            raise ValueError("单工具任务不能包含 first_subtask")

        if not self.is_single and self.first_subtask is None:
            raise ValueError("多工具任务必须包含 first_subtask")

        return self
        
        
class ToolSelection(BaseModel):
    model_config = ConfigDict(
        extra="forbid",
        strict=True,
        str_strip_whitespace=True,
    )

    tool_name: str | None = Field(
        min_length=1,
        description="选中的候选工具内部名称；没有合适工具时为 null",
    )
    

class ParameterExtraction(BaseModel):
    model_config = ConfigDict(
        extra="forbid",
        strict=True,
    )

    parameters: dict[str, JsonValue] = Field(
        description="按工具参数名称提取出的参数；未提取到时为空字典",
    )
    
    
class SecurityDecision(BaseModel):
    model_config = ConfigDict(
        extra="forbid",
        strict=True,
        str_strip_whitespace=True,
    )

    should_block: bool = Field(
        description="是否阻止本次工具调用",
    )
    reason: str | None = Field(
        default=None,
        min_length=1,
        description="阻止调用的原因；允许调用时可以为空",
    )

    @model_validator(mode="after")
    def require_block_reason(self) -> Self:
        if self.should_block and self.reason is None:
            raise ValueError("阻止工具调用时必须提供 reason")
        return self
        
        
class HumanIntent(BaseModel):
    model_config = ConfigDict(
        extra="forbid",
        strict=True,
    )

    intent: Literal["confirm", "abort", "unclear"] = Field(
        description="用户对当前工具调用的确认意图",
    )


class NextTaskDecision(BaseModel):
    model_config = ConfigDict(
        extra="forbid",
        strict=True,
        str_strip_whitespace=True,
    )

    is_complete: bool = Field(
        description="用户的整个请求是否已完成",
    )
    next_subtask: str | None = Field(
        default=None,
        min_length=1,
        description="未完成时需要执行的下一个子任务；完成时必须为空",
    )

    @model_validator(mode="after")
    def validate_next_task(self) -> Self:
        if self.is_complete and self.next_subtask is not None:
            raise ValueError("任务已完成时不能包含 next_subtask")

        if not self.is_complete and self.next_subtask is None:
            raise ValueError("任务未完成时必须包含 next_subtask")

        return self


class SummaryResult(BaseModel):
    model_config = ConfigDict(
        extra="forbid",
        strict=True,
        str_strip_whitespace=True,
    )

    answer: str = Field(
        min_length=1,
        description="根据工具执行结果生成的最终回答",
    )


    