from langchain_openai import ChatOpenAI

class ModelFactory:
    @staticmethod
    def create_chat_model(
        model_name,
        base_url,
        api_key,
        temperature,
        top_p,
        timeout=60,
        max_retries=2,
        model_class=ChatOpenAI,
    ):
        return model_class(
            model=model_name,
            base_url=base_url,
            api_key=api_key,
            temperature=temperature,
            top_p=top_p,
            timeout=timeout,
            max_retries=max_retries,
        )

    