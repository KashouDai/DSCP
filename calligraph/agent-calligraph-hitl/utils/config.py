import os

from dotenv import load_dotenv

# 加载.env文件中的环境变量
load_dotenv()



# 从环境变量中读取配置，如果不存在则使用默认值
milvus_uri = os.getenv("milvus_uri", "http://localhost:19530")
milvus_db_name = os.getenv("milvus_db_name", "tool_db")
model_top_p = float(os.getenv("model_top_p", "0.01"))
mongo_host = os.getenv("mongo_host", "127.0.0.1")
mongo_db = os.getenv("mongo_db", "tools")
mongo_port = int(os.getenv("mongo_port", "27017"))
model_path = os.getenv("model_path", "model")
model_name = os.getenv("model_name", "qwen3.7-flash-2026-07-15")
model_temperature = float(os.getenv("model_temperature", "0.01"))
topK = int(os.getenv("topK", "5"))
model_api_key = os.getenv("model_api_key")
model_base_url = os.getenv("model_base_url")
sim_api_key = os.getenv("sim_api_key")
SECRET_KEY = os.getenv('SECRET_KEY', '<your-secret-key>')
JWT_ALGORITHM = os.getenv('JWT_ALGORITHM', 'HS256')

local_mode = int(os.getenv("local_mode", "0"))
if local_mode:
    milvus_uri = "http://localhost:19530"
    mongo_host = "127.0.0.1"
if  not model_api_key:
    model_api_key = os.getenv("DASHSCOPE_API_KEY")
if  not model_base_url:
    model_base_url = "https://dashscope.aliyuncs.com/compatible-mode/v1"
if not model_api_key:
    print("警告：未配置 model_api_key，Agent 的模型调用能力暂不可用。")
