from workflow.feature_flags import select_workflow


class WorkflowRouter:
    def __init__(self, legacy_runner, graph_runner=None, workflow_name=None):
        self.legacy_runner = legacy_runner
        self.graph_runner = graph_runner
        self.workflow_name = workflow_name

    def submit(self, *args, **kwargs):
        selected = self.workflow_name or select_workflow()
        if selected == "langgraph" and self.graph_runner is not None:
            return self.graph_runner(*args, **kwargs)
        return self.legacy_runner(*args, **kwargs)
