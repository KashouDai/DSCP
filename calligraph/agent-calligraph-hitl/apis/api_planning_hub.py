import json
import os

from apis.api_selection_hub import ApiSelectionHub
from models import LargeLanguageModel
from param_extraction.param_extraction_hub import ParamExtractionHub
from tasks import TaskManager,GenerateTaskHub
from tools import ToolSummaryHub, ToolUseHub, ToolManager
from utils import logger, TASK_ERROR_CODE, TASK_SUCCESS_CODE, RESPONSE_STATUS_CODE_SUCCESS, TASK_STATUS_FINISH, \
    TASK_STATUS_RUNNING, TASK_STATUS_WAIT_CONFIRM, TASK_STATUS_WAIT_INPUT, TASK_INIT_TOOL_ID, TASK_TYPE_SINGLE, TASK_TYPE_APIS, \
    TASK_TYPE_UNKNOWN, TASK_TYPE_MAINTAIN, GRAPH_TITLE_SUCESS, GRAPH_TITLE_FAILURE, TASK_SYS_OUTPUT_STOP
import traceback
import uuid
import re
from copy import deepcopy
from datetime import date

from utils.tool_result import normalize_response, result_text
from utils.digital_sales_catalog import DigitalSalesCatalog


class ApiPlanningHub:
    def __init__(self, milvus_uri, model_path, milvus_db_name, model, temperature, top_p,
                mongo_host, mongo_db, mongo_port, topK, api_url, api_key,executor,
                user_authorization=None):
        """
        初始化 ApiPlanningHub 类的实例。

        :param milvus_uri: 连接地址，通常用于指定服务的访问地址
        :param model_path: 模型文件的路径
        :param milvus_db_name: 数据库的名称
        :param model: 使用的LLM名称
        :param temperature: 采样温度，用于控制生成文本的随机性
        :param top_p: 核采样概率，用于控制生成文本的多样性
        :param mongo_host: 主机地址，通常用于数据库或服务的连接
        :param mongo_db: 数据库标识，可能用于指定具体的数据库
        :param mongo_port: 端口号，用于网络连接
        :param topK: 检索时返回的前 K 个结果
        """
        logger.info("初始化 ApiPlanningHub: ApiSelectionHub")
        self.api_selection_hub = ApiSelectionHub(milvus_uri, model_path, milvus_db_name, model, temperature, top_p,
                                                mongo_host, mongo_db, mongo_port, api_url, api_key)
        logger.info("初始化 ApiPlanningHub: ApiSelectionHub 完成")
        self.topK = topK
        self.param_extraction_hub = ParamExtractionHub(model, temperature, top_p, api_url, api_key)
        logger.info("初始化 ApiPlanningHub: ParamExtractionHub 完成")
        self.tool_summary_hub = ToolSummaryHub(model, temperature, top_p, api_url, api_key)
        self.tool_use_hub = ToolUseHub("")
        self.tool_use_hub.set_user_authorization(user_authorization)
        self.generate_task_hub = GenerateTaskHub(model, temperature, top_p, api_url, api_key, mongo_host, mongo_db, mongo_port, milvus_uri, milvus_db_name)
        self.task_manager = TaskManager(mongo_host, mongo_db, mongo_port)
        logger.info("初始化 ApiPlanningHub: ToolManager")
        self.tool_manager = ToolManager(mongo_host, mongo_db, mongo_port, milvus_uri, milvus_db_name)
        logger.info("初始化 ApiPlanningHub: ToolManager 完成")
        self.executor = executor
        self.llm = LargeLanguageModel(api_url, api_key)
        self.model_api_key = api_key
        self.model = model
        self.temperature = temperature
        self.top_p = top_p

    def generate_output(self, text):
        prompt = " 请将以下句子进行润色成通顺的话，请直接输出结果：\n\n\n" + text
        print(prompt)
        result = self.llm.chat_completions(prompt, self.model, self.temperature, self.top_p)
        return result

    def _summarize_read_result(self, tool, data, fallback):
        """将只读工具结果整理为用户可读文本，失败时保留原始摘要。"""
        try:
            data = self._unwrap_result_data(data)
            display_data = json.loads(json.dumps(data, ensure_ascii=False))
            self._translate_status(display_data, self._status_mapping(tool.operationId))
            source = json.dumps(display_data, ensure_ascii=False, separators=(",", ":"))
            prompt = (
                f"你是数字销售系统助手。请将以下{tool.name_for_human}查询结果整理成简洁、易读的中文。"
                "只保留用户关心的业务字段，使用短句或项目符号，不要输出JSON、工具名、代码、推理过程。"
                "状态码必须翻译成中文含义：客户状态0为正常、1为停用；商品状态0为上架、1为下架；"
                "订单、售后和退款状态也要优先使用系统中的中文业务含义，无法确认时保留原值并注明状态码。"
                "如果结果为空，明确说明没有找到记录。\n查询结果：" + source[:12000]
            )
            text = self.llm.chat_completions(
                prompt, self.model, self.temperature, self.top_p,
                timeout=float(os.getenv("MODEL_SUMMARY_TIMEOUT_SECONDS", "6")), retries=1
            )
            text = text.strip() if isinstance(text, str) else ""
            if not text or text.startswith("{") or text.startswith("["):
                return fallback
            return text
        except Exception as exception:
            logger.warning(f"整理只读结果失败，回退结构化摘要: {exception}")
            return self._local_read_summary(tool, data)

    def _unwrap_result_data(self, data):
        while isinstance(data, dict) and isinstance(data.get("data"), (dict, list)):
            data = data["data"]
        return data

    def _local_read_summary(self, tool, data):
        """模型不可用时生成不含原始 JSON 的本地摘要。"""
        if tool.operationId == "analytics_query":
            metric_labels = {
                "customer_count": "客户数量", "order_count": "订单数量",
                "order_amount_sum": "订单金额合计", "order_amount_avg": "平均订单金额",
                "refund_count": "退款数量", "refund_amount_sum": "退款金额合计",
                "net_sales_amount": "订单金额减退款金额", "low_stock_count": "低库存商品数量",
                "customer_order_summary": "各客户订单统计", "order_status_summary": "各订单状态统计",
                "customer_business_summary": "各客户业务统计",
                "monthly_order_amount_summary": "按月销售金额统计",
            }
            value = data.get("value") if isinstance(data, dict) else None
            label = metric_labels.get(data.get("metric") if isinstance(data, dict) else None, "统计结果")
            if isinstance(data, dict) and isinstance(data.get("rows"), list):
                rows = data["rows"]
                if not rows:
                    return f"{label}：暂无数据。"
                lines = []
                for row in rows:
                    if "customerName" in row:
                        values = [f"订单 {row.get('orderCount', 0)} 笔", f"订单金额 {row.get('orderAmount', 0)} 元"]
                        if "refundAmount" in row:
                            values.append(f"退款金额 {row.get('refundAmount', 0)} 元")
                        if "netSalesAmount" in row:
                            values.append(f"净销售额 {row.get('netSalesAmount', 0)} 元")
                        lines.append(f"- {row.get('customerName')}：" + "，".join(values))
                    elif "month" in row:
                        lines.append(f"- {row.get('month')}：{row.get('orderAmount', 0)}元（{row.get('orderCount', 0)}笔）")
                    else:
                        lines.append(f"- {row.get('status') or '未设置状态'}：{row.get('orderCount', 0)}条，{row.get('orderAmount', 0)}元")
                return label + "：\n" + "\n".join(lines)
            return f"{label}：{value if value is not None else 0}。"
        labels = {
            "customerName": "客户", "customerId": "客户ID", "phone": "电话", "email": "邮箱",
            "address": "地址", "orderNo": "订单号", "orderId": "订单ID", "totalAmount": "金额",
            "status": "状态", "createTime": "创建时间", "completeTime": "完成时间",
            "updateTime": "更新时间", "orderNo": "订单号", "ticketNo": "售后单号",
            "ticketId": "售后单ID", "refundId": "退款单ID", "refundAmount": "退款金额",
            "refundedAmount": "已退款金额", "productName": "商品", "skuName": "规格",
            "customerPhone": "客户电话", "handleTime": "处理时间", "handlerName": "处理人",
        }
        status = self._status_mapping(tool.operationId)

        def lines(item):
            result = []
            if not isinstance(item, dict):
                return result
            for key, value in item.items():
                if key in labels and value not in (None, ""):
                    value = status.get(str(value), value) if key == "status" else value
                    result.append(f"- {labels[key]}：{value}")
            return result

        rows = data.get("rows") if isinstance(data, dict) else None
        if isinstance(rows, list):
            total = data.get("total", len(rows))
            business_names = {
                "customer_list": "客户信息",
                "order_list": "销售订单",
                "after_sale_list": "售后记录",
                "refund_list": "退款记录",
            }
            result = [f"已查询到{business_names.get(tool.operationId, tool.name_for_human)}，共{total}条记录。"]
            for row in rows[:20]:
                row_lines = lines(row)
                if row_lines:
                    result.append("\n".join(row_lines))
            return "\n\n".join(result)
        result = lines(data)
        return "查询结果：\n" + "\n".join(result) if result else "查询完成，但没有可展示的业务字段。"

    def _status_mapping(self, operation):
        mappings = {
            "customer_list": {"0": "正常", "1": "停用"},
            "product_list": {"0": "上架", "1": "下架"},
            "order_list": {
                "PENDING": "待确认", "DRAFT": "草稿", "CONFIRMED": "已确认",
                "COMPLETED": "已完成", "CANCELLED": "已取消",
            },
            "after_sale_list": {
                "PENDING": "待处理", "PROCESSING": "处理中", "COMPLETED": "已完成",
                "CLOSED": "已关闭", "CANCELLED": "已取消",
            },
            "refund_list": {
                "PENDING": "待处理", "PROCESSING": "处理中", "REFUNDED": "已退款",
                "APPROVED": "已通过", "REJECTED": "已拒绝", "CANCELLED": "已取消",
            },
        }
        return mappings.get(operation, {})

    def _filter_read_result(self, data, entity_name=None, customer_id=None):
        if (not entity_name and customer_id is None) or not isinstance(data, dict) or not isinstance(data.get("rows"), list):
            return data
        rows = [row for row in data["rows"] if (
            customer_id is not None and str(row.get("customerId", "")) == str(customer_id)
        ) or (
            entity_name and str(row.get("customerName", "")) == str(entity_name)
        )]
        filtered = dict(data)
        filtered["rows"] = rows
        filtered["total"] = len(rows)
        return filtered

    def _translate_status(self, value, mapping):
        if isinstance(value, dict):
            if "status" in value and str(value["status"]) in mapping:
                value["status"] = mapping[str(value["status"])]
            for child in value.values():
                self._translate_status(child, mapping)
        elif isinstance(value, list):
            for child in value:
                self._translate_status(child, mapping)

    def _set_task_type(self, query, task_id, task_type, system_output):
        logger.debug(f"用户请求[{query}]生成任务[{task_id}]{system_output}")
        self.task_manager.update_task_recorder(task_id, TASK_STATUS_RUNNING, system_output, task_type=task_type)

    def _update_task_curr_desc(self,task_id,task_desc):
        self.task_manager.update_task_recorder(task_id, TASK_STATUS_RUNNING, "正在为您分析中，请稍等......",
                                                graph_title="正在为您分析中，请稍等......", curr_task_desc=task_desc)

    def _update_task_node_edge(self, task, task_new_result, system_output,is_end=False):
        """
        更新显示的知识图谱推理路径
        :param task: 当前任务的Task实体
        :param task_new_result: 任务的当前结果，结果是个字典，包含以下键值对
                {
                "code": ,
                "result": "",
                "tool": "",
                "missing_param": [],
                "param": [],
                "query": "",
                "task_description": ""
            }
        :param system_output: 输出的信息
        :param is_end: 当前任务是否结束
        """
        try:
            nodes = task.nodes
            sub_nodes = []
            edges = task.edges
            index = len(nodes)
            sub_index = index * 10000
            graph_title = GRAPH_TITLE_SUCESS
            # if system_output == "当前API无法实现用户需求":
            #     self.taskManager.update_task_recorder(task.task_id, TASK_STATUS_FINISH, system_output,  is_end,
            #                                         TASK_TYPE_MAINTAIN,[], [], "异常调用链")
            #     return
            if task_new_result["code"] != TASK_SUCCESS_CODE:
                self.task_manager.update_task_recorder(task.task_id, TASK_STATUS_FINISH, TASK_SYS_OUTPUT_STOP+system_output, graph_title=GRAPH_TITLE_FAILURE)
                return
            nodes.append({
                "id": str(index),
                "name": str(index) + "_" + task_new_result["tool"],
                "label": task_new_result["tool"],
                "group": task_new_result["tool"],
                "task_description": task_new_result["query"],
                "params": json.dumps(task_new_result["param"], ensure_ascii=False),
                "result": json.dumps(task_new_result["result"], ensure_ascii=False),
            })
            if(len(nodes) > 1):
                edges.append(
                    {
                        "source": nodes[index-1]["id"], "target": nodes[index]["id"], "value": '正向API规划', "symbolSize": [5, 20],
                        "label": {"show": False}
                    }
                )
            for param_node in task_new_result["missing_param"]:
                sub_nodes.append({
                    "id": str(sub_index),
                    "name": str(sub_index) + "_" + param_node["tool"],
                    "label": param_node["tool"],
                    "group": param_node["tool"],
                    "task_description": param_node["task_description"],
                    "params": json.dumps(param_node["param"], ensure_ascii=False),
                    "result": json.dumps(param_node["result"], ensure_ascii=False),
                })
                edges.append(
                    {
                        "source": str(sub_index), "target": str(index), "value": '缺省参数逆向API规划',
                        "symbolSize": [5, 20],
                        "label": {"show": False}
                    }
                )
                sub_index += 1

            nodes = nodes + sub_nodes
        except Exception as e:
            logger.error(f"任务{task.task_id}图像绘制错误：{e}，但任务继续进行...\n{traceback.format_exc()}")
            self.task_manager.update_task_recorder(task.task_id, TASK_STATUS_RUNNING, "图像绘制错误，但任务继续进行...", graph_title="图像绘制失败")
            return
        logger.debug(f"任务{task.task_id}保存图像的节点[{nodes}]和边[{edges}]")
        next_status = TASK_STATUS_FINISH if is_end else TASK_STATUS_RUNNING
        self.task_manager.update_task_recorder(
            task.task_id, next_status, system_output, graph_title,
            nodes=nodes, edges=edges,
            result_payload=task_new_result.get("result_payload"))
        return

    def _not_loop_validate(self, task,cur_task_result):
        """
        检测API调用链中是否存在循环调用。主要逻辑如下：
        遍历API调用链，跟踪当前API名称和连续相同结果的计数
        当遇到不同API时重置计数器
        当连续3次出现相同API且结果相同时，判定为循环调用并返回False
        如果没有检测到循环则返回True
        该函数通过比较相邻API调用的结果来识别可能的无限循环情况。
        """
        api_chains = []
        nodes = task.nodes
        for node in nodes:
            api_chains.append({
                "tool": node["label"],
                "result": node["result"],
            })
        api_chains.append({
            "tool": cur_task_result["tool"],
            "result": cur_task_result["result"],
        })
        current_api_name = ""
        number = 0
        for i in range(len(api_chains)):
            api_response = api_chains[i]
            if api_response["tool"] != current_api_name:
                current_api_name = api_response["tool"]
                number = 1
            else:
                if api_chains[i - 1]["result"] == api_response["result"]:
                    number += 1
                if number == 3:
                    return False
        return True

    def _supplement_parameters(self, new_query, missing_param):
        """
        补充缺失的参数值。
        该方法根据新的查询语句和缺失的参数名，尝试从工具调用的结果中获取缺失的参数值。
        首先通过 `apiSelectionHub` 获取合适的工具，然后使用 `paramExtractionHub` 提取参数，
        调用工具并解析返回结果，若结果中包含缺失的参数，则返回该参数值和工具名称。
        输入：
            :param new_query: 新的查询语句，用于获取缺失参数的值
            :param missing_param: 缺失的参数名
        :return:
            若找到缺失参数的值，返回参数值和工具名称、汇总响应结果；否则返回3个 None
        """
        #缺失的参数有可能是其他工具调用的结果
        tool = self.api_selection_hub.get_tool_coarse_and_fine(new_query, None, topK=self.topK)
        if tool is None:
            return None, None, None
        params, new_missing_param = self.param_extraction_hub.extraction_params(new_query, tool)
        if len(new_missing_param) != 0:
            return None, None, None

        single_tool_response = self.tool_use_hub.tool_use(tool, params)

        success, normalized = normalize_response(single_tool_response)
        if not success:
            return None, None, None

        results = normalized.get("data")
        if isinstance(results, dict) and "rows" in results:
            results = results.get("rows") or []
        if isinstance(results, list):
            matches = [item[missing_param] for item in results
                       if isinstance(item, dict) and missing_param in item]
            if len(matches) == 1:
                return matches[0], tool.name_for_human, {
                    "code": TASK_SUCCESS_CODE,
                    "tool": tool.name_for_human,
                    "result": result_text(normalized),
                    "param": params,
                    "query": new_query,
                    "task_description": new_query
                }
        elif isinstance(results, dict) and missing_param in results:
            return results[missing_param], tool.name_for_human, {
                "code": TASK_SUCCESS_CODE,
                "tool": tool.name_for_human,
                "result": result_text(normalized),
                "param": params,
                "query": new_query,
                "task_description": new_query
            }
        return None, None, None

    def _resolve_order_items(self, items, query):
        """Resolve natural-language order items to the SKU ID and current price."""
        if not isinstance(items, list) or not items:
            return None, "订单明细不能为空"
        resolved = []
        for item in items:
            if not isinstance(item, dict):
                return None, "订单明细格式不正确"
            quantity = item.get("quantity")
            product_name = item.get("productName") or item.get("product")
            sku_name = item.get("skuName") or item.get("specification") or item.get("spec")
            if item.get("skuId") is not None and item.get("salePrice") is not None:
                resolved.append({"skuId": item["skuId"], "salePrice": item["salePrice"], "quantity": quantity})
                continue
            if not quantity or not (product_name or sku_name):
                return None, "订单明细缺少商品、规格或数量"
            lookup_query = f"查询商品{product_name or ''}的规格{sku_name or ''}"
            lookup_tool = self.api_selection_hub.get_tool_coarse_and_fine(lookup_query, None, topK=self.topK)
            if lookup_tool is None or lookup_tool.operationId not in ("sku_list", "stock_list"):
                return None, f"无法查询商品规格：{product_name or sku_name}"
            lookup_params, lookup_missing = self.param_extraction_hub.extraction_params(lookup_query, lookup_tool)
            if lookup_missing:
                return None, f"无法解析商品规格：{product_name or sku_name}"
            success, normalized = normalize_response(self.tool_use_hub.tool_use(lookup_tool, lookup_params))
            rows = normalized.get("data", {}).get("rows", []) if success else []
            matches = [row for row in rows if isinstance(row, dict)
                       and (not product_name or row.get("productName") == product_name)
                       and (not sku_name or row.get("skuName") == sku_name)]
            if len(matches) != 1:
                return None, f"商品规格“{product_name or ''} {sku_name or ''}”匹配到{len(matches)}条，请提供更准确的规格"
            row = matches[0]
            resolved.append({"skuId": row.get("skuId"), "salePrice": row.get("salePrice"), "quantity": quantity})
        return resolved, None

    def _tool_check(self, tool, task_desc, raw_query):
        """
        对工具的检查，此函数接收工具和用户查询语句

        主要工作：
        1. 检查工具是否缺乏参数。
        2. 结合用户查询语句和工具，检查是否存在提示词注入攻击。
        3. 如工具缺乏参数，借助大模型进行参数的补全。

        :param
            tool: 当前被检查的工具
            task_desc: 输入的查询语句，用于描述用户的需求(由大模型生成和改写)
            raw_query：用户的原始查询
        :return:
            一个字典，包含处理结果的相关信息，
            如状态码code、工具名称tool、结果内容result、缺失参数信息missing_param、参数列表param和任务描述task_description等
        """
        params, missing_params = self.param_extraction_hub.extraction_params(task_desc + " " + raw_query, tool)
        # 列表查询的分页参数属于执行默认值，不应再次交给模型补全。
        parameter_names = {parameter.name for parameter in tool.request_body}
        if "pageNum" in parameter_names and not params.get("pageNum"):
            params["pageNum"] = 1
        if "pageSize" in parameter_names and not params.get("pageSize"):
            params["pageSize"] = 20
        missing_params = [item for item in missing_params if item.name not in ("pageNum", "pageSize")]
        if tool.operationId == "stock_outbound" and not params.get("remark"):
            params["remark"] = f"Agent操作：{raw_query}"
            missing_params = [item for item in missing_params if item.name != "remark"]
        for parameter in tool.request_body:
            if parameter.source == "systemGenerated":
                params[parameter.name] = f"agent-{uuid.uuid4().hex}"
                missing_params = [item for item in missing_params if item.name != parameter.name]
        if any(parameter.name == "items" for parameter in missing_params):
            return self._missing_parameters_result(tool, task_desc, params, missing_params)
        if tool.operationId == "order_create" and params.get("items"):
            resolved_items, resolve_error = self._resolve_order_items(params["items"], task_desc + " " + raw_query)
            if resolve_error:
                return {
                    "code": TASK_ERROR_CODE,
                    "result": "order_items_unresolved",
                    "tool": "异常调用节点-订单明细解析失败",
                    "missing_param": [],
                    "param": params,
                    "query": task_desc,
                    "task_description": resolve_error
                }
            params["items"] = resolved_items
            missing_params = [item for item in missing_params if item.name != "items"]
        missing_params_supplemented = []
        new_params = params.copy()
        if len(missing_params) == 0:
            logger.debug(f"[{raw_query}:{task_desc}]未缺失参数，准备安全检查")
            inject_flag, reason = self.generate_task_hub.gen_judge_task(task_desc, tool, new_params)
            if inject_flag:
                return {
                    "code": TASK_ERROR_CODE,
                    "result": "inject",
                    "tool": "异常调用节点-提示注入攻击",
                    "missing_param": [],
                    "param": {},
                    "query": task_desc,
                    "task_description": reason
                }
        else:
            logger.debug(f"[{raw_query}:{task_desc}]缺失参数，进行参数的补齐....")

            for missing_param in missing_params:
                gen_param_query = self.generate_task_hub.gen_param_task(task_desc,
                                                                        json.dumps(params, ensure_ascii=False, indent=4),
                                                        f'{missing_param.name}: {missing_param.description}')
                logger.debug(f"[{task_desc}]参数[{missing_param.name}:{missing_param.description}]生成或补齐任务的描述词为：{gen_param_query}")
                supplement_param, supplement_param_tool, supplement_param_result = self._supplement_parameters(
                    gen_param_query, missing_param.name)
                if supplement_param is not None:
                    logger.debug(f"[{raw_query}:{task_desc}]的缺失参数{missing_param.name}已补充：\n{supplement_param_tool}\n{supplement_param_result}")
                    params[missing_param.name] = supplement_param
                    missing_params_supplemented.append(supplement_param_result)
                else:
                    return self._missing_parameters_result(tool, task_desc, params, [missing_param])

            logger.debug(f"[{raw_query}:{task_desc}]参数已补全，进行安全检查")
            inject_flag, reason = self.generate_task_hub.gen_judge_task(task_desc, tool, params)
            if inject_flag:
                return {
                    "code": TASK_ERROR_CODE,
                    "result": "inject",
                    "tool": "异常调用节点-提示注入攻击",
                    "missing_param": [],
                    "param": {},
                    "query": task_desc,
                    "task_description": reason
                }

        return {
            "code": TASK_SUCCESS_CODE,
            "result": "",
            "tool": tool.name_for_human,
            "missing_param": missing_params_supplemented,
            "param": params,
            "query": task_desc,
            "task_description": task_desc
        }

    def _missing_parameters_result(self, tool, task_desc, params, missing_params):
        hints = {
            "customerId": ("客户", "客户ID，也可以提供客户姓名或手机号", "客户张三，手机号13800000000"),
            "items": ("订单明细", "商品或规格及购买数量", "智能健康手表银色版 2 件"),
            "skuId": ("SKU", "商品规格ID", "SKU 9005"),
            "quantity": ("数量", "大于0的操作数量", "2 件"),
        }
        fields = []
        for parameter in missing_params:
            label, description, example = hints.get(
                parameter.name,
                (parameter.description or parameter.name, parameter.description or parameter.name, f"请提供{parameter.description or parameter.name}")
            )
            fields.append({"name": parameter.name, "label": label, "description": description, "example": example})
        lines = [f"{index + 1}. {field['label']}：{field['description']}（例如：{field['example']}）"
                 for index, field in enumerate(fields)]
        return {
            "code": TASK_ERROR_CODE,
            "result": "missing_param",
            "tool": tool.name_for_human,
            "missing_param": fields,
            "param": params,
            "query": task_desc,
            "task_description": "还需要以下信息：\n" + "\n".join(lines) + "\n请直接回复上述信息，我会继续处理。"
        }


    def _build_confirmation_payload(self, tool, params):
        payload = {"tool": tool.name_for_human, "operation": tool.operationId, "params": params}
        if tool.operationId == "order_create":
            items = params.get("items", [])
            payload["summary"] = {
                "title": "创建订单", "customerId": params.get("customerId"), "items": items,
                "totalAmount": sum(float(item.get("salePrice", 0)) * int(item.get("quantity", 0)) for item in items),
            }
        elif tool.operationId in ("stock_inbound", "stock_outbound"):
            payload["summary"] = {
                "title": "入库" if tool.operationId == "stock_inbound" else "出库",
                "skuId": params.get("skuId"), "quantity": params.get("quantity"), "remark": params.get("remark"),
            }
        return payload

    def api_planning_before_human_feedback(self, task_desc, task_id, raw_query):
        """
        单API规划步骤中人类反馈部分前工作内容。此函数接收一个查询语句，根据查询选择合适的工具。
        如果在处理过程中出现问题或找不到合适的工具，则返回相应的错误信息。

        代码流程：
        1. 通过 `apiSelectionHub` 根据查询语句获取合适的工具。
        2. 若未找到工具，返回包含错误信息的结果。
        3. 若找到工具，使用 `paramExtractionHub` 提取参数和缺失参数。
        5. 若有缺失参数，使用 `generateTaskHub` 生成新查询，调用 `supplement_Parameters` 补充缺失参数。
        6. 若所有缺失参数都补充成功，并返回工具和工具参数；若有缺失参数补充失败，返回错误信息。

        :param
            task_desc: 输入的查询语句，用于描述用户的需求，一般多API时使用
            task_id: 任务ID
            raw_query：用户的原始查询
        :return:
            一个字典，包含处理结果的相关信息，如状态码、工具名称、结果内容、缺失参数信息、参数列表和任务描述等
        """
        tool = self.api_selection_hub.get_tool_coarse_and_fine(task_desc, None, topK=self.topK)

        if tool is None:
            txt = f"您的要求[{raw_query}]未找到合适的工具，请换个问法或问题再试试。"
            logger.warning(txt)
            self.task_manager.update_task_recorder(task_id, TASK_STATUS_FINISH, TASK_SYS_OUTPUT_STOP+txt, graph_title=GRAPH_TITLE_FAILURE)
        else:
            result = self._tool_check(tool, task_desc, raw_query)
            if result["code"] == TASK_SUCCESS_CODE:
                if not tool.isValidate:
                    self.task_manager.update_task_recorder(
                        task_id, TASK_STATUS_RUNNING, "正在查询业务数据...",
                        curr_task_desc=task_desc, curr_tool_id=tool.tool_id, curr_tool_param=result["param"]
                    )
                    task = self.task_manager.get_task_by_id(task_id)
                    invoke_result = self._process_single_api_invoke(task_desc, task, tool, result["param"])
                    if invoke_result.get("code") == TASK_SUCCESS_CODE:
                        self.task_manager.update_task_recorder(
                            task_id, TASK_STATUS_FINISH, invoke_result["result"], graph_title="查询完成",
                            result_payload=invoke_result.get("result_payload")
                        )
                    else:
                        self.task_manager.update_task_recorder(
                            task_id, TASK_STATUS_FINISH,
                            TASK_SYS_OUTPUT_STOP + invoke_result["task_description"],
                            graph_title=GRAPH_TITLE_FAILURE,
                            result_payload=invoke_result.get("result_payload")
                        )
                    return
                system_output = f'''根据您的查询要求{raw_query}，我发现目前需要使用工具{tool.name_for_human}，
                相关参数是[{result["param"]}]。\n请确认该工具是否正确且立即使用。如果您想停止本次任务执行也请告诉我。
                \n如果该工具正确且立即使用，建议回答‘立即执行’;
                \n如果您想停止本次任务执行，建议回答‘不执行’;
                '''
                self.task_manager.update_task_recorder(task_id, TASK_STATUS_WAIT_CONFIRM, system_output, graph_title="请确认",
                                                        curr_task_desc=task_desc,curr_tool_id=tool.tool_id, curr_tool_param=result["param"],
                                                        confirmation_payload=self._build_confirmation_payload(tool, result["param"]))

            else:
                if result.get("result") == "missing_param":
                    self.task_manager.update_task_recorder(
                        task_id, TASK_STATUS_WAIT_INPUT, result["task_description"], graph_title="等待补充信息",
                        curr_task_desc=task_desc, curr_tool_id=tool.tool_id, curr_tool_param=result.get("param") or {},
                        result_payload={"type": "missing_parameters", "tool": tool.operationId,
                                        "originalQuery": raw_query, "fields": result.get("missing_param") or []}
                    )
                else:
                    self.task_manager.update_task_recorder(task_id, TASK_STATUS_FINISH, TASK_SYS_OUTPUT_STOP+result["task_description"], graph_title=GRAPH_TITLE_FAILURE, )
                # return result

    def api_planning_handle_human_feedback(self, task, human_feedback):
        """
        用以处理人类反馈信息，
        :param
            task: 任务实例
            human_feedback: 人类反馈信息
        """
        try:
            if task.status != TASK_STATUS_WAIT_CONFIRM:
                logger.warning(f"任务[{task.task_id}]当前状态[{task.status}]不可重复确认")
                return

            # 获取当前等待确认的工具信息
            curr_tool_id = task.curr_tool_id
            curr_tool_param = task.curr_tool_param
            
            if curr_tool_id == TASK_INIT_TOOL_ID:
                logger.error(f"任务[{task.task_id}]没有等待确认的工具")
                self.task_manager.update_task_recorder(task.task_id, TASK_STATUS_FINISH,
                                                    TASK_SYS_OUTPUT_STOP+"没有等待确认的工具",
                                                    graph_title=GRAPH_TITLE_FAILURE)
                return
            
            # 获取工具对象
            tool = self.tool_manager.get_tools_by_ids([curr_tool_id])[0]
            
            # 进行意图识别
            intent = self._recognize_human_intent(human_feedback, tool, curr_tool_param)
            
            # 根据意图执行不同操作
            if intent == "confirm":
                # 确认执行工具调用
                # 分任务类型处理工具调用结果
                if task.task_type == TASK_TYPE_SINGLE:
                    invoke_result = self._process_single_api_invoke(task.raw_query,task, tool, curr_tool_param)
                    if invoke_result["code"] == TASK_ERROR_CODE:
                        # self._update_task_node_edge(task, invoke_result, invoke_result["task_description"])
                        self.task_manager.update_task_recorder(task.task_id, TASK_STATUS_FINISH,
                                                                TASK_SYS_OUTPUT_STOP + invoke_result["task_description"],
                                                                graph_title=GRAPH_TITLE_FAILURE)
                        return
                    if tool.operationId == "order_create":
                        verified, order_id = self._verify_created_order(invoke_result)
                        if not verified:
                            self.task_manager.update_task_recorder(
                                task.task_id, TASK_STATUS_FINISH,
                                TASK_SYS_OUTPUT_STOP + "订单创建接口已返回，但订单结果校验失败，请到订单列表确认后再重试。",
                                graph_title=GRAPH_TITLE_FAILURE)
                            return
                        invoke_result["task_description"] = f"订单已创建并核验成功，订单ID：{order_id}"
                    summary = self.tool_summary_hub.tool_summary(task.changed_query, [invoke_result])
                    self._update_task_node_edge(task, invoke_result,summary,True)
                    # self.task_manager.update_task_recorder(task.task_id, TASK_STATUS_FINISH, summary, graph_title="任务完成")
                    logger.info(f"任务[{task.changed_query}]，ID[{task.task_id}]：已完成，任务摘要[{summary}]")
                else:
                    invoke_result = self._process_single_api_invoke(task.curr_task_desc,task, tool, curr_tool_param)
                    result = invoke_result["task_description"]
                    if invoke_result["code"] == TASK_ERROR_CODE:
                        logger.info(f"任务[{task.changed_query}]，ID[{task.task_id}]中止：发生错误，[{result}]")
                        self.task_manager.update_task_recorder(task.task_id, TASK_STATUS_FINISH, result,graph_title="任务错误")
                        return
                    if not self._not_loop_validate(task,invoke_result):
                        logger.info(f"任务[{task.changed_query}]，ID[{task.task_id}]中止：发生调用链错误-循环调用")
                        invoke_result["code"] = TASK_ERROR_CODE
                        self.task_manager.update_task_recorder(task.task_id, TASK_STATUS_FINISH,TASK_SYS_OUTPUT_STOP+"循环调用错误",
                                                                graph_title="调用链错误-循环调用")
                        return
                    logger.info(f"任务[{task.changed_query}]，ID[{task.task_id}]：已处理完成子任务{task.curr_task_desc}")
                    self._update_task_node_edge(task, invoke_result, f"已处理完成子任务 {task.curr_task_desc}")

                    # 任务类型为多API调用任务时，除了处理本次调用结果，还要继续下一个API调用
                    flag, task_description = self.generate_task_hub.gen_from_context_task(task.changed_query, task.nodes)
                    logger.info(f"任务[{task.changed_query}]，ID[{task.task_id}]：任务是否完成：[{flag}]，后续任务[{task_description}]")
                    if task_description is not None:
                        logger.debug(f"任务[{task.changed_query}]，ID[{task.task_id}]继续，任务描述[{task_description}]入库")
                        self._update_task_curr_desc(task.task_id, task_description)
                    # if not flag and task_description is not None:
                        self.api_planning_before_human_feedback(task_description, task.task_id, task.raw_query)
                    # if task_description is not None:
                    #     self.api_planning_before_human_feedback(task_description, task.task_id, task.raw_query)
                    #多API调用任务也完成
                    if flag and task_description is None:
                        context = self._get_summary_from_nodes(task)
                        summary = self.tool_summary_hub.tool_summary(task.changed_query, context)
                        self.task_manager.update_task_recorder(task.task_id, TASK_STATUS_FINISH, summary, graph_title="任务完成")

            elif intent == "abort":
                # 放弃任务执行
                self.task_manager.update_task_recorder(task.task_id, TASK_STATUS_FINISH, TASK_SYS_OUTPUT_STOP+"已为您放弃任务执行")
            elif intent == "unclear":
                # 意图不明确，再次请求确认
                system_output = f'''您的反馈"{human_feedback}"不够明确，请重新确认：
                当前需要使用工具{tool.name_for_human}，相关参数是{curr_tool_param}，
                请明确是否执行该工具调用，或者确认放弃本次任务执行。'''
                self.task_manager.update_task_recorder(task.task_id, TASK_STATUS_WAIT_CONFIRM, system_output)
            else:
                # 其他情况，继续等待确认
                system_output = f'''您的反馈"{human_feedback}"我们暂时无法理解，请重新确认：
                当前需要使用工具{tool.name_for_human}，相关参数是{curr_tool_param}，
                请明确是否执行该工具调用，或者确认放弃本次任务执行。'''
                self.task_manager.update_task_recorder(task.task_id, TASK_STATUS_WAIT_CONFIRM, system_output)
        except Exception as e:
            logger.error(f"处理人类反馈时发生错误: {e}\n{traceback.format_exc()}")
            self.task_manager.update_task_recorder(task.task_id, TASK_STATUS_FINISH, TASK_SYS_OUTPUT_STOP+"处理您的要求时发生系统内部错误，请联系系统管理员", graph_title=GRAPH_TITLE_FAILURE)

    def _process_single_api_invoke(self,query,task,tool,params):
        logger.debug(f"[{query}]准备进行工具{tool.operationId}调用,参数为：{params}")
        single_tool_response = self.tool_use_hub.tool_use(tool, params)
        logger.debug(f"[{query}]工具{tool.operationId}调用完成,响应为：{single_tool_response}")
        success, normalized = normalize_response(single_tool_response)
        if not success:
            error_message = normalized.get("errorMessage") or "调用外部系统失败"
            result = {
                "code": TASK_ERROR_CODE,
                "result": result_text(normalized),
                "tool": "异常调用节点-调用外部系统失败",
                "missing_param": [],
                "param": params,
                "query": query,
                "task_description": error_message,
                "result_payload": normalized
            }
        else:
            display_result = result_text(normalized)
            if tool.operationId in {"customer_list", "order_list", "after_sale_list", "refund_list"}:
                data = self._unwrap_result_data(normalized.get("data"))
                if tool.operationId in {"after_sale_list", "refund_list"}:
                    data = self._filter_read_result(
                        data, params.get("customerName"), params.get("customerId")
                    )
                display_result = self._summarize_read_result(
                    tool, data, self._local_read_summary(tool, data)
                )
                result_payload = data
                if tool.operationId in {"after_sale_list", "refund_list"}:
                    result_payload = self._filter_read_result(result_payload, getattr(task, "entity_name", None))
                result_payload = json.loads(json.dumps(result_payload, ensure_ascii=False))
                self._translate_status(result_payload, self._status_mapping(tool.operationId))
                if isinstance(result_payload, dict):
                    rows = result_payload.get("rows")
                    total = result_payload.get("total")
                    has_data = bool(rows) if isinstance(rows, list) else bool(result_payload)
                    result_payload["queryStatus"] = "has_data" if has_data or total else "no_data"
            elif tool.operationId == "analytics_query":
                data = self._unwrap_result_data(normalized.get("data"))
                display_result = self._local_read_summary(tool, data)
                result_payload = data
            else:
                result_payload = normalized.get("data")
            result = {
                "code": TASK_SUCCESS_CODE,
                "tool": tool.name_for_human,
                "result": display_result,
                "missing_param": [],
                "param": params,
                "query": query,
                "task_description": query,
                "result_payload": result_payload
            }
        logger.debug(f"[{query}]工具{tool.operationId}调用返回值：{result}")
        return result

    def _verify_created_order(self, invoke_result):
        """创建订单后查询订单详情，确认写入结果确实可读。"""
        payload = invoke_result.get("result_payload")
        while isinstance(payload, dict) and isinstance(payload.get("data"), (dict, list)):
            payload = payload["data"]
        order_id = payload.get("orderId") if isinstance(payload, dict) else None
        if order_id is None and isinstance(payload, dict):
            order_id = payload.get("id")
        if order_id is None:
            return False, None
        order_tool = next((item for item in DigitalSalesCatalog().to_tools() if item.operationId == "order_get"), None)
        if order_tool is None:
            return False, order_id
        success, detail = normalize_response(self.tool_use_hub.tool_use(order_tool, {"orderId": order_id}))
        return success and bool(detail.get("data")), order_id

    def _get_summary_from_nodes(self,task):
        context = []
        nodes = task.nodes
        for node in nodes:
            step = {
                "tool": node["label"],
                "task_description": node["task_description"],
                "result": node["result"]
            }
            context.append(step)
        return context

    def _recognize_human_intent(self, human_feedback, tool, tool_param):
        """
        识别人类反馈的意图
        :param human_feedback: 人类反馈信息
        :param tool: 当前工具
        :param tool_param: 工具参数
        :return: 意图类型
        """
        # 简易关键词匹配，此处仅做示例，实际场景中需要根据实际情况进行修改或者错误率较高时可以直接取消
        confirm_keywords = ["确认执行", "执行任务", "同意执行", "继续执行", "立即执行"]
        abort_keywords = ["放弃执行", "停止执行", "中止执行", "取消执行", "不执行", "不要执行"]

        feedback_lower = human_feedback.lower()
        
        # 确认意图
        for keyword in confirm_keywords:
            if keyword in feedback_lower:
                return "confirm"
        
        # 放弃意图
        for keyword in abort_keywords:
            if keyword in feedback_lower:
                return "abort"
        
        # 使用大模型进行语义理解
        prompt = f"""
        请分析以下人类反馈的意图，从以下选项中选择一个最符合的意图类型：
        1. confirm: 确认执行当前工具调用
        2. abort: 放弃任务执行
        3. unclear: 意图不明确
        
        当前工具: {tool.name_for_human}
        工具参数: {tool_param}
        人类反馈: {human_feedback}
        
        请只回答意图类型，不要包含其他内容。
        """
        
        try:
            llm = self.api_selection_hub.LargeLanguageModel
            response = llm.chat_completions(prompt, self.api_selection_hub.model,
                                            self.api_selection_hub.temperature, self.api_selection_hub.top_p)
            intent = response.strip().lower()
            if intent in ["confirm", "abort", "unclear"]:
                return intent
        except Exception as e:
            logger.error(f"使用大模型识别意图时出错: {e}\n{traceback.format_exc()}")
        
        return "unclear"

    def apis_planning(self, query, task_id):
        """
        多API规划函数，用于处理单个或多个查询的API调用流程。
        此函数接收一个查询语句，根据查询判断是否为单任务。若为单任务，直接调用单API规划函数；
        若为多任务，则循环调用单API规划函数，直到任务处理完成。最终返回API调用结果链。

        代码流程：
        1. 调用 `generateTaskHub.gen_root_task` 判断是否为单任务并获取根任务描述。
        2. 若为单任务，调用 `single_api_planning` 处理并将结果添加到结果链中。
        3. 若为多任务，进入循环，不断调用 `single_api_planning` 处理任务，
        并通过 `generateTaskHub.gen_task_from_context` 判断是否还有后续任务。
        4. 若最后还有剩余任务描述，再次调用 `single_api_planning` 处理并添加到结果链中。
        5. 返回完整的API调用结果链。

        :param
            query:输入的查询语句，用于用户描述的需求
            task_id:当前任务内部编号
        :return:
            一个列表，包含一个或多个字典，每个字典表示一次API调用的处理结果，
            包含状态码、工具名称、结果内容、缺失参数信息、参数列表和任务描述等
        """

        if re.fullmatch(r"[你您]好|嗨|您好|谢谢|感谢|在吗", query.strip()):
            reply = "你好，我可以帮你查询客户、订单、库存、售后和退款，也可以发起需要确认的业务操作。"
            if re.search(r"谢谢|感谢", query.strip()):
                reply = "不客气。"
            self.task_manager.update_task_recorder(task_id, TASK_STATUS_FINISH, reply, graph_title="对话完成")
            return

        if re.fullmatch(r"(?:随便)?(?:看|查)(?:一?下|一?看|一?查)?(?:吧|呢)?", re.sub(r"\s+", "", query or "")):
            self.task_manager.update_task_recorder(
                task_id, TASK_STATUS_FINISH,
                "请说明你想查询的内容，例如：客户信息、订单、售后记录、退款记录或低库存商品。",
                graph_title="等待明确查询内容")
            return

        if re.search(r"(?i)\b(select|insert|update|delete|drop|truncate|alter)\b|删除|修改|新增|更新|清空", query or ""):
            self.task_manager.update_task_recorder(
                task_id, TASK_STATUS_FINISH,
                "仅支持通过业务语句进行只读查询，不支持原始SQL或删除、修改数据操作。",
                graph_title="查询被拒绝")
            return

        if re.search(r"任意字段|未知字段|字段\s+[A-Za-z_][A-Za-z0-9_.]*", query or ""):
            self.task_manager.update_task_recorder(
                task_id, TASK_STATUS_FINISH,
                "无法识别查询字段，请使用系统支持的客户、订单、售后、退款、商品或库存字段。",
                graph_title="查询被拒绝")
            return

        limit_match = re.search(r"(?:返回|查询|显示)\s*(\d+)\s*条", query or "")
        if limit_match and int(limit_match.group(1)) > 100:
            self.task_manager.update_task_recorder(
                task_id, TASK_STATUS_FINISH,
                "单次查询最多返回100条记录，请缩小查询范围或分批查询。",
                graph_title="查询被拒绝")
            return

        analytics = self._match_analytics_query(query)
        global_metric_query = re.fullmatch(
            r"(?:查询|查看|请问|帮我查)?(?:退款总金额|退款金额合计|退款总额|"
            r"订单金额减退款金额|订单总额减退款总额|净销售额|"
            r"各订单状态分别有多少条|各订单状态有多少条|订单状态分别有多少|"
            r"订单总金额|订单金额合计|订单总额)(?:是多少|有多少)?[？?]?",
            re.sub(r"\s+", "", query or ""),
        )
        if analytics and (global_metric_query or analytics["metric"] == "customer_business_summary"):
            self._handle_analytics_query(analytics, query, task_id)
            return

        # 已有确定性规则的查询先使用稳定计划；未命中规则的复杂问题再交给模型。
        read_plan = self._match_grouped_read_query_plan(query) or self._match_read_query_plan(query)
        if read_plan:
            self._handle_read_query_plan(read_plan, query, task_id)
            return

        # 未命中确定性规则时交给模型生成 QueryPlan；模型不可用或输出不合法时继续回退。
        model_first = os.getenv("MODEL_QUERY_PLANNER_FIRST", "true").lower() not in {"0", "false", "no"}
        model_read_plan = self._match_model_read_query_plan(query) if model_first else None
        if model_read_plan:
            self._handle_read_query_plan(model_read_plan, query, task_id)
            return

        if not model_first:
            model_read_plan = self._match_model_read_query_plan(query)
            if model_read_plan:
                self._handle_read_query_plan(model_read_plan, query, task_id)
                return

        analytics = self._match_analytics_query(query)
        if analytics:
            self._handle_analytics_query(analytics, query, task_id)
            return

        if self._match_customer_count_query(query):
            self._handle_customer_count_query(query, task_id)
            return

        global_read = self._match_global_read_query(query)
        if global_read:
            self._handle_global_read_query(global_read, query, task_id)
            return

        missing_scope = self._match_missing_scope_query(query)
        if missing_scope:
            self._handle_missing_scope_query(missing_scope, query, task_id)
            return

        customer_filter = self._match_customer_filter_query(query)
        if customer_filter:
            field, value = customer_filter
            self._handle_customer_list_query(query, task_id, {field: value})
            return

        if self._match_customer_list_query(query):
            self._handle_customer_list_query(query, task_id)
            return

        if self._match_low_stock_query(query):
            self._handle_low_stock_query(query, task_id)
            return

        multi_query = self._match_multi_customer_read_query(query)
        if multi_query:
            self._handle_multi_customer_read(multi_query, query, task_id)
            return

        numeric_customer_scope = self._match_numeric_customer_scope_query(query)
        if numeric_customer_scope:
            self.task_manager.update_task_recorder(
                task_id, TASK_STATUS_WAIT_INPUT,
                "该类客户业务查询支持客户姓名、联系电话或邮箱，请提供其中一项；当前不支持使用客户ID查询。",
                graph_title="等待补充客户姓名",
                result_payload={"type": "missing_customer_name", "operation": numeric_customer_scope}
            )
            return

        clarified_query = self._match_clarified_customer_query(query)
        if clarified_query:
            tool, params = self._resolve_customer_order_query(clarified_query, task_id, query)
            if tool is None:
                return
            self._set_task_type(query, task_id, TASK_TYPE_SINGLE, "系统已确认查询业务")
            self.task_manager.update_task_recorder(
                task_id, TASK_STATUS_RUNNING, "正在查询业务数据...",
                curr_task_desc=query, curr_tool_id=tool.tool_id, curr_tool_param=params
            )
            task = self.task_manager.get_task_by_id(task_id)
            result = self._process_single_api_invoke(query, task, tool, params)
            if result.get("code") == TASK_SUCCESS_CODE:
                self.task_manager.update_task_recorder(
                    task_id, TASK_STATUS_FINISH, result["result"], graph_title="查询完成",
                    result_payload=result.get("result_payload")
                )
            else:
                self.task_manager.update_task_recorder(
                    task_id, TASK_STATUS_FINISH, TASK_SYS_OUTPUT_STOP + result["task_description"],
                    graph_title=GRAPH_TITLE_FAILURE, result_payload=result.get("result_payload")
                )
            return

        clarification = self._match_ambiguous_customer_query(query)
        if clarification:
            self._set_task_type(query, task_id, TASK_TYPE_SINGLE, "系统需要确认查询业务")
            self.task_manager.update_task_recorder(
                task_id, TASK_STATUS_WAIT_INPUT, clarification["question"], graph_title="等待确认查询业务",
                result_payload=clarification
            )
            return

        order_tool = self._match_bare_order_create(query)
        if order_tool:
            missing_params = [parameter for parameter in order_tool.request_body
                              if parameter.required and parameter.source != "systemGenerated"]
            result = self._missing_parameters_result(order_tool, query, {}, missing_params)
            self._set_task_type(query, task_id, TASK_TYPE_SINGLE, f"系统识别[{query}]为创建订单任务")
            self.task_manager.update_task_recorder(
                task_id, TASK_STATUS_WAIT_INPUT, result["task_description"], graph_title="等待补充信息",
                curr_task_desc=query, curr_tool_id=order_tool.tool_id,
                result_payload={"type": "missing_parameters", "tool": order_tool.operationId,
                                "originalQuery": query, "fields": result["missing_param"]}
            )
            return

        explicit_order = self._match_explicit_order_create(query)
        if explicit_order:
            self._prepare_explicit_order_create(query, task_id, explicit_order)
            return

        category_create = self._match_explicit_category_create(query)
        if category_create:
            self._prepare_explicit_category_create(query, task_id, category_create)
            return

        fast_tool = self._match_fast_stock_tool(query)
        if fast_tool:
            self._set_task_type(query, task_id, TASK_TYPE_SINGLE, f"系统识别[{query}]为库存单工具调用任务")
            params = self._extract_fast_stock_params(query, fast_tool)
            self.task_manager.update_task_recorder(
                task_id, TASK_STATUS_WAIT_CONFIRM,
                f"已识别库存{('入库' if fast_tool.operationId == 'stock_inbound' else '出库')}操作，等待确认",
                graph_title="请确认", curr_task_desc=query, curr_tool_id=fast_tool.tool_id,
                curr_tool_param=params, confirmation_payload=self._build_confirmation_payload(fast_tool, params)
            )
            return

        order_action = self._match_fast_order_action(query)
        if order_action:
            tool, params = order_action
            self._set_task_type(query, task_id, TASK_TYPE_SINGLE, "系统已识别订单状态操作，等待确认")
            self.task_manager.update_task_recorder(
                task_id, TASK_STATUS_WAIT_CONFIRM, f"已识别订单{tool.name_for_human}操作，等待确认",
                graph_title="请确认", curr_task_desc=query, curr_tool_id=tool.tool_id,
                curr_tool_param=params, confirmation_payload=self._build_confirmation_payload(tool, params)
            )
            return

        service_action = self._match_fast_service_action(query)
        if service_action:
            tool, params = service_action
            self._set_task_type(query, task_id, TASK_TYPE_SINGLE, "系统已识别售后或退款操作，等待确认")
            self.task_manager.update_task_recorder(
                task_id, TASK_STATUS_WAIT_CONFIRM, f"已识别{tool.name_for_human}操作，等待确认",
                graph_title="请确认", curr_task_desc=query, curr_tool_id=tool.tool_id,
                curr_tool_param=params, confirmation_payload=self._build_confirmation_payload(tool, params)
            )
            return

        is_single_task, root_task_description = self.generate_task_hub.gen_root_task(query)
        logger.debug(f"系统初始处理[{query}]，is_single_task={is_single_task},任务描述为：{root_task_description}")
        if is_single_task:
            self._set_task_type(query, task_id, TASK_TYPE_SINGLE, f"系统判定[{query}]为单工具调用任务")
            self.api_planning_before_human_feedback(query, task_id, query)
        else:
            self._set_task_type(query, task_id, TASK_TYPE_APIS, f"系统判定[{query}]多工具调用任务")
            self.api_planning_before_human_feedback(root_task_description, task_id, query)
        return

    def _match_bare_order_create(self, query):
        normalized = re.sub(r"[，。！？\s]", "", query)
        if normalized not in ("创建订单", "新建订单", "帮我创建订单", "帮我新建订单"):
            return None
        from utils.digital_sales_catalog import DigitalSalesCatalog
        return next((tool for tool in DigitalSalesCatalog().to_tools() if tool.operationId == "order_create"), None)

    def _match_explicit_category_create(self, query):
        """识别已提供分类名称和显示顺序的分类新增请求。"""
        normalized = query.replace("：", ":").replace("，", ",")
        if not re.search(r"创建(?:商品)?分类|新增(?:商品)?分类", normalized):
            return None
        name = re.search(r"分类名称\s*[:：]?\s*([^,，。]+)", normalized)
        order = re.search(r"(?:显示顺序|排序|顺序)\s*[:：]?\s*(\d+)", normalized)
        if not (name and order):
            return None
        status_match = re.search(r"状态\s*[:：]?\s*(正常|启用|停用|禁用|[01])", normalized)
        status = "1" if status_match and status_match.group(1) in ("停用", "禁用", "1") else "0"
        return {"categoryName": name.group(1).strip(), "orderNum": int(order.group(1)), "status": status}

    def _prepare_explicit_category_create(self, query, task_id, params):
        tools = {tool.operationId: tool for tool in DigitalSalesCatalog().to_tools()}
        tool = tools.get("category_create")
        if tool is None:
            return
        self._set_task_type(query, task_id, TASK_TYPE_SINGLE, "系统已生成商品分类草案，等待确认")
        self.task_manager.update_task_recorder(
            task_id, TASK_STATUS_WAIT_CONFIRM,
            "已识别商品分类新增信息，请确认是否创建。",
            graph_title="请确认", curr_task_desc=query, curr_tool_id=tool.tool_id,
            curr_tool_param=params, confirmation_payload=self._build_confirmation_payload(tool, params),
        )

    def _match_explicit_order_create(self, query):
        """识别已经提供客户、SKU和数量的明确创建订单请求。"""
        normalized = query.replace("：", ":").replace("，", ",")
        if not re.search(r"(?:创建|新建)订单", normalized):
            return None
        customer = re.search(r"客户(?:ID|编号)?\s*[:：]?\s*(\d+)", normalized, re.IGNORECASE)
        sku = re.search(r"SKU\s*[:：]?\s*(\d+)", normalized, re.IGNORECASE)
        quantity = re.search(r"数量\s*[:：]?\s*(\d+)\s*(?:件|个|台|套)?", normalized)
        if not (customer and sku and quantity):
            return None
        quantity_value = int(quantity.group(1))
        if quantity_value <= 0:
            return {"invalid": "数量必须大于0"}
        return {"customerId": int(customer.group(1)), "skuId": int(sku.group(1)), "quantity": quantity_value}

    def _prepare_explicit_order_create(self, query, task_id, values):
        """查询真实客户和SKU，生成订单确认草案，不执行写入。"""
        if values.get("invalid"):
            self.task_manager.update_task_recorder(
                task_id, TASK_STATUS_FINISH, TASK_SYS_OUTPUT_STOP + values["invalid"],
                graph_title=GRAPH_TITLE_FAILURE)
            return
        tools = {tool.operationId: tool for tool in DigitalSalesCatalog().to_tools()}
        customer_result = self._lookup_order_reference(tools["customer_get"], {"customerId": values["customerId"]})
        if not customer_result[0]:
            self.task_manager.update_task_recorder(
                task_id, TASK_STATUS_FINISH, TASK_SYS_OUTPUT_STOP + f"未找到客户ID {values['customerId']}，无法创建订单。",
                graph_title=GRAPH_TITLE_FAILURE)
            return
        sku_result = self._lookup_order_reference(tools["sku_get"], {"skuId": values["skuId"]})
        if not sku_result[0]:
            self.task_manager.update_task_recorder(
                task_id, TASK_STATUS_FINISH, TASK_SYS_OUTPUT_STOP + f"未找到SKU {values['skuId']}，无法创建订单。",
                graph_title=GRAPH_TITLE_FAILURE)
            return

        customer = customer_result[1]
        sku = sku_result[1]
        params = {
            "customerId": values["customerId"],
            "requestNo": f"agent-{uuid.uuid4().hex}",
            "items": [{
                "skuId": values["skuId"],
                "productName": sku.get("productName"),
                "skuName": sku.get("skuName"),
                "salePrice": sku.get("salePrice"),
                "quantity": values["quantity"],
            }],
        }
        order_tool = tools["order_create"]
        self._set_task_type(query, task_id, TASK_TYPE_SINGLE, "系统已生成订单草案，等待确认")
        self.task_manager.update_task_recorder(
            task_id, TASK_STATUS_WAIT_CONFIRM,
            "已核对客户和商品规格，订单草案已生成，请确认是否创建。",
            graph_title="请确认", curr_task_desc=query, curr_tool_id=order_tool.tool_id,
            curr_tool_param=params,
            confirmation_payload=self._build_confirmation_payload(order_tool, params),
        )

    def _lookup_order_reference(self, tool, params):
        success, normalized = normalize_response(self.tool_use_hub.tool_use(tool, params))
        if not success:
            return False, {}
        data = normalized.get("data")
        while isinstance(data, dict) and isinstance(data.get("data"), (dict, list)):
            data = data["data"]
        return isinstance(data, dict) and bool(data), data if isinstance(data, dict) else {}

    def handle_structured_read(self, task, selection, entity_name):
        """执行澄清卡片已确定的只读工具，跳过重复的模型规划。"""
        raw_query = task.raw_query or ""
        name_match = re.search(
            r"(?:查询|查|帮我查|帮我查询)(?:客户)?([\u4e00-\u9fa5]{2,6}?)(?:的客户信息|客户信息|的信息|信息)(?:\s|$)",
            raw_query
        )
        if name_match:
            entity_name = name_match.group(1)
        elif not entity_name:
            match = re.search(r"(?:查询|查)(?:客户)?([\u4e00-\u9fa5]{2,6})", raw_query)
            entity_name = match.group(1) if match else None
        if entity_name:
            entity_name = re.sub(r"(?:客户|的信息|信息|的)$", "", str(entity_name)).strip()
        if not entity_name:
            self.task_manager.update_task_recorder(
                task.task_id, TASK_STATUS_FINISH,
                TASK_SYS_OUTPUT_STOP + "没有识别出要查询的客户，请重新输入客户姓名。",
                graph_title=GRAPH_TITLE_FAILURE
            )
            return True
        operation_map = {
            "customer_get": ("customer_list", {"pageNum": 1, "pageSize": 20, "customerName": entity_name}),
            "order_list": ("order_list", {"pageNum": 1, "pageSize": 20, "customerName": entity_name}),
            "after_sale_list": ("after_sale_list", {}),
            "refund_list": ("refund_list", {}),
        }
        selected = operation_map.get(selection)
        if not selected:
            return False
        operation, params = selected
        tool = next((item for item in DigitalSalesCatalog().to_tools() if item.operationId == operation), None)
        if tool is None:
            return False
        result = self._process_single_api_invoke(task.raw_query, task, tool, params)
        if result.get("code") == TASK_SUCCESS_CODE and selection in {"after_sale_list", "refund_list"}:
            filtered = self._filter_read_result(result.get("result_payload"), entity_name)
            result["result_payload"] = json.loads(json.dumps(filtered, ensure_ascii=False))
            self._translate_status(result["result_payload"], self._status_mapping(operation))
            result["result"] = self._local_read_summary(tool, filtered)
        if result.get("code") != TASK_SUCCESS_CODE:
            self.task_manager.update_task_recorder(
                task.task_id, TASK_STATUS_FINISH, TASK_SYS_OUTPUT_STOP + result["task_description"],
                graph_title=GRAPH_TITLE_FAILURE, result_payload=result.get("result_payload")
            )
            return True
        self.task_manager.update_task_recorder(
            task.task_id, TASK_STATUS_FINISH, result["result"], graph_title="查询完成",
            result_payload=result.get("result_payload")
        )
        return True

    def _match_ambiguous_customer_query(self, query):
        """识别只给出客户姓名、但没有说明查询业务的请求。"""
        match = re.search(
            r"^(?:帮我)?(?:查(?:下|一下)?|给我)?\s*([\u4e00-\u9fa5]{2,6}?)(?:的)?(?:客户)?(?:信息|资料)?\s*$",
            query.strip()
        )
        if not match:
            return None
        customer_name = match.group(1)
        if customer_name in {"全部", "所有", "客户", "订单", "售后", "退款"}:
            return None
        return {
            "type": "clarification",
            "reason": "intent_ambiguous",
            "entityType": "customer",
            "entityName": customer_name,
            "question": f"你想查询{customer_name}的哪类信息？",
            "options": [
                {"label": "客户信息", "value": "customer_get"},
                {"label": "销售订单", "value": "order_list"},
                {"label": "售后记录", "value": "after_sale_list"},
                {"label": "退款记录", "value": "refund_list"},
            ],
            "allowFreeText": True,
        }

    def _match_clarified_customer_query(self, query):
        """将明确的客户查询映射到只读工具。"""
        match = re.search(r"(?:查询|查|看下|查看|看看|再看下|再看看|帮我看下|帮我查)(?:客户(?:ID\s*)?)?([\u4e00-\u9fa5]{2,6}?|\d{7,}|[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,})的(客户信息|客户资料|销售订单|订单|售后记录|退款记录)", query)
        if not match:
            return None
        from utils.digital_sales_catalog import DigitalSalesCatalog
        operation = {"客户信息": "customer_list", "客户资料": "customer_list", "销售订单": "order_list", "订单": "order_list", "售后记录": "after_sale_list", "退款记录": "refund_list"}[match.group(2)]
        tool = next((item for item in DigitalSalesCatalog().to_tools() if item.operationId == operation), None)
        if tool is None:
            return None
        value = match.group(1)
        params = {"pageNum": 1, "pageSize": 20}
        if re.fullmatch(r"1\d{10}", value):
            params["customerPhone"] = value
        elif value.isdigit():
            params["customerId"] = int(value)
        elif "@" in value:
            params["customerEmail"] = value
        else:
            params["customerName"] = value
        return tool, params

    def _match_customer_count_query(self, query):
        """识别不带筛选条件的客户数量查询。"""
        return bool(re.fullmatch(
            r"\s*(?:请问|帮我)?(?:目前|现在)?(?:一共有|共有|有)?(?:多少个客户|客户有多少个|客户总数)(?:呢|吗)?[？?]?\s*",
            query or ""
        ))

    def _match_customer_phone_query(self, query):
        """识别客户手机号查询，确保手机号作为接口筛选条件传递。"""
        normalized = re.sub(r"\s+", "", (query or "").strip())
        match = re.fullmatch(r"(?:查询|查看|查一下|查下)?(?:客户)?(?:手机号|电话)?(1\d{10})(?:的)?(?:客户信息|客户资料)?", normalized)
        return match.group(1) if match else None

    def _match_numeric_customer_scope_query(self, query):
        normalized = re.sub(r"\s+", "", (query or "").strip())
        if not re.search(r"客户\d+的(?:订单|退款(?:记录)?|售后(?:记录)?|订单金额|退款金额)", normalized):
            return None
        if "退款" in normalized:
            return "refund_list"
        if "售后" in normalized:
            return "after_sale_list"
        return "order_list"

    def _match_customer_filter_query(self, query):
        normalized = re.sub(r"\s+", "", (query or "").strip())
        normalized = re.sub(r"[？?。！!]$", "", normalized)
        phone = self._match_customer_phone_query(normalized)
        if phone:
            return "phone", phone
        email = re.search(r"[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}", normalized)
        if email:
            return "email", email.group(0)
        return None

    def _handle_customer_count_query(self, query, task_id):
        """直接读取客户列表总数，避免统计问题进入通用模型规划和确认链路。"""
        tool = next((item for item in DigitalSalesCatalog().to_tools()
                     if item.operationId == "customer_list"), None)
        if tool is None:
            self.task_manager.update_task_recorder(
                task_id, TASK_STATUS_FINISH, TASK_SYS_OUTPUT_STOP + "客户查询工具不可用。",
                graph_title=GRAPH_TITLE_FAILURE
            )
            return
        self._set_task_type(query, task_id, TASK_TYPE_SINGLE, "正在统计客户数量")
        task = self.task_manager.get_task_by_id(task_id)
        result = self._process_single_api_invoke(
            query, task, tool, {"pageNum": 1, "pageSize": 1}
        )
        if result.get("code") != TASK_SUCCESS_CODE:
            self.task_manager.update_task_recorder(
                task_id, TASK_STATUS_FINISH, TASK_SYS_OUTPUT_STOP + result["task_description"],
                graph_title=GRAPH_TITLE_FAILURE, result_payload=result.get("result_payload")
            )
            return
        payload = result.get("result_payload") or {}
        total = payload.get("total", 0) if isinstance(payload, dict) else 0
        self.task_manager.update_task_recorder(
            task_id, TASK_STATUS_FINISH, f"当前共有 {total} 位客户。",
            graph_title="查询完成", result_payload={"total": total, "rows": []}
        )

    def _match_customer_list_query(self, query):
        """识别不带筛选条件的客户列表查询。"""
        return bool(re.fullmatch(
            r"\s*(?:请问|帮我(?:查(?:一下|下)?)?|查(?:一下|下)?|查询|查看|看看)?(?:公司|系统)?(?:目前|现在)?(?:所有客户|全部客户|所有客户信息|全部客户信息|所有客户资料|全部客户资料|有哪些客户|客户有哪些|客户总数|客户)(?:呢|吗|是多少)?[？?]?\s*",
            query or ""
        ))

    def _match_missing_scope_query(self, query):
        normalized = re.sub(r"\s+", "", (query or "").strip())
        normalized = re.sub(r"[？?。！!]$", "", normalized)
        rules = [
            ("customer_list", ("查询客户信息", "查看客户信息", "查询客户资料", "查看客户资料", "客户信息"), "请提供客户姓名、客户ID、手机号或邮箱；如果要查询全部客户，请回复“查询所有客户”。"),
            ("order_list", ("查询订单", "查看订单", "查询销售订单"), "请提供订单号、订单ID或客户姓名；如果要查询全部订单，请回复“查询全部订单”。"),
            ("order_list", ("查询某客户的订单", "查询客户的订单"), "请提供客户姓名；如果要查询全部订单，请回复“查询全部订单”。"),
            ("after_sale_list", ("查询售后记录", "查看售后记录", "查询售后", "售后记录"), "请提供售后单号、订单号、客户姓名或客户ID；如果要查询全部售后记录，请回复“查询全部售后记录”。"),
            ("refund_list", ("查询退款记录", "查看退款记录", "查询退款"), "请提供退款单号、订单号、客户姓名或客户ID；如果要查询全部退款记录，请回复“查询全部退款记录”。"),
        ]
        for operation, phrases, question in rules:
            if normalized in phrases:
                return {"operation": operation, "question": question}
        return None

    def _match_global_read_query(self, query):
        normalized = re.sub(r"\s+", "", (query or "").strip())
        normalized = re.sub(r"[？?。！!]$", "", normalized)
        mapping = {
            "查询所有客户": "customer_list", "查询全部客户": "customer_list",
            "查询所有客户信息": "customer_list", "查询全部客户信息": "customer_list",
            "查询所有订单": "order_list", "查询全部订单": "order_list", "查看所有订单": "order_list",
            "所有订单": "order_list", "全部订单": "order_list",
            "查询所有售后记录": "after_sale_list", "查询全部售后记录": "after_sale_list",
            "所有售后记录": "after_sale_list", "全部售后记录": "after_sale_list",
            "查询所有退款记录": "refund_list", "查询全部退款记录": "refund_list",
            "所有退款记录": "refund_list", "全部退款记录": "refund_list",
        }
        return mapping.get(normalized)

    def _handle_global_read_query(self, operation, query, task_id):
        tool = next((item for item in DigitalSalesCatalog().to_tools()
                     if item.operationId == operation), None)
        if tool is None:
            self.task_manager.update_task_recorder(
                task_id, TASK_STATUS_FINISH, TASK_SYS_OUTPUT_STOP + "查询工具不可用。",
                graph_title=GRAPH_TITLE_FAILURE
            )
            return
        task = self.task_manager.get_task_by_id(task_id)
        result = self._process_single_api_invoke(
            query, task, tool, {"pageNum": 1, "pageSize": 100}
        )
        self.task_manager.update_task_recorder(
            task_id, TASK_STATUS_FINISH,
            result["result"] if result.get("code") == TASK_SUCCESS_CODE
            else TASK_SYS_OUTPUT_STOP + result["task_description"],
            graph_title="查询完成" if result.get("code") == TASK_SUCCESS_CODE else GRAPH_TITLE_FAILURE,
            result_payload=result.get("result_payload")
        )

    def _handle_missing_scope_query(self, missing_scope, query, task_id):
        self._set_task_type(query, task_id, TASK_TYPE_SINGLE, "等待补充查询范围")
        self.task_manager.update_task_recorder(
            task_id, TASK_STATUS_WAIT_INPUT, missing_scope["question"],
            graph_title="等待补充查询范围",
            result_payload={
                "type": "missing_scope", "operation": missing_scope["operation"],
                "originalQuery": query,
            }
        )

    def _handle_customer_list_query(self, query, task_id, filters=None):
        """直接查询客户列表并按客户名称返回，避免进入通用模型规划。"""
        tool = next((item for item in DigitalSalesCatalog().to_tools()
                     if item.operationId == "customer_list"), None)
        if tool is None:
            self.task_manager.update_task_recorder(
                task_id, TASK_STATUS_FINISH, TASK_SYS_OUTPUT_STOP + "客户查询工具不可用。",
                graph_title=GRAPH_TITLE_FAILURE
            )
            return
        self._set_task_type(query, task_id, TASK_TYPE_SINGLE, "正在查询客户列表")
        task = self.task_manager.get_task_by_id(task_id)
        params = {"pageNum": 1, "pageSize": 100}
        if filters:
            params.update(filters)
        result = self._process_single_api_invoke(
            query, task, tool, params
        )
        if result.get("code") != TASK_SUCCESS_CODE:
            self.task_manager.update_task_recorder(
                task_id, TASK_STATUS_FINISH, TASK_SYS_OUTPUT_STOP + result["task_description"],
                graph_title=GRAPH_TITLE_FAILURE, result_payload=result.get("result_payload")
            )
            return
        payload = result.get("result_payload") or {}
        rows = payload.get("rows", []) if isinstance(payload, dict) else []
        if filters:
            local_filters = {key: value for key, value in filters.items() if key == "email"}
            for key, value in local_filters.items():
                rows = [row for row in rows if str(value).lower() in str(row.get(key) or "").lower()]
            if local_filters and isinstance(payload, dict):
                payload["rows"] = rows
                payload["total"] = len(rows)
        names = list(dict.fromkeys(row.get("customerName") for row in rows
                                  if isinstance(row, dict) and row.get("customerName")))
        total = payload.get("total", len(rows)) if isinstance(payload, dict) else len(rows)
        if filters and rows:
            text = self._local_read_summary(tool, payload)
        elif not rows:
            text = "当前没有客户记录。"
        else:
            text = f"当前共有 {total} 条客户记录，已展示 {len(rows)} 条：" + "、".join(names)
            if total > len(rows):
                text += "。其余记录未在本次结果中展开。"
        self.task_manager.update_task_recorder(
            task_id, TASK_STATUS_FINISH, text,
            graph_title="查询完成", result_payload={"total": total, "rows": rows}
        )

    def _match_low_stock_query(self, query):
        """识别低于库存预警值的商品查询。"""
        return bool(re.fullmatch(
            r"\s*-?\s*(?:请问|帮我(?:查(?:一下|下)?)?|查(?:一下|下)?|查询|查看|看看)?(?:当前)?(?:(?:库存)?(?:低于|少于|达到或低于)预警值的?(?:商品|SKU|规格)?|库存不足(?:的)?(?:商品|SKU|规格)?|低库存(?:的)?(?:商品|SKU|规格)?)(?:有哪些|列表|有多少个)?[？?]?\s*",
            query or ""
        ))

    def _match_model_read_query_plan(self, query):
        """由模型将未命中规则的只读请求转换为受限 QueryPlan。"""
        if not self.model_api_key or not re.search(r"查询|统计|多少|排行|前\d+|看下|查看", query or ""):
            return None
        base_prompt = """你是只读业务查询规划器。把用户请求转换为单个 JSON 对象，不能输出其他文字。
仅允许 subject: customer, sale_order, after_sale, refund, sku。
仅允许字段：customer.id, customer.name, customer.phone, customer.email, customer.status,
order.id, order.no, order.totalAmount, order.status, order.createTime, order.confirmTime, order.completeTime, order.month,
afterSale.no, afterSale.status, afterSale.type, afterSale.createTime,
refund.id, refund.amount, refund.status, refund.createTime, refund.handleTime。
sku.id, sku.code, sku.name, sku.stock, sku.warnStock, sku.status, product.name, product.status。
仅允许 metrics：order_count, order_amount_sum, order_amount_avg, refund_count, refund_amount_sum, after_sale_count, customer_count, sku_count, low_stock_count。未明确指定客户且问题是全局统计时，不要添加 customer.identity；只有客户条件会影响结果且缺失时才使用 missingFields:["customer.identity"]。
filter.operator 仅允许 eq, ne, gt, gte, lt, lte, contains, between, in, is_null, not_null。
只读，绝不输出 SQL。缺少必要客户条件时，使用 missingFields:[\"customer.identity\"]，不要猜测值。
JSON 仅可含 subject, detail, fields, filters, metrics, groupBy, orderBy, limit, missingFields。
业务口径：订单数量使用 COUNT，订单金额使用有效订单金额合计，退款金额使用实际退款金额；“今年”使用当前年度起止时间。
用户请求：""" + query
        allowed = {"subject", "detail", "fields", "filters", "metrics", "groupBy", "orderBy", "limit", "missingFields"}
        allowed_subjects = {"customer", "sale_order", "after_sale", "refund", "sku"}
        allowed_fields = {
            "customer.id", "customer.name", "customer.phone", "customer.email", "customer.status",
            "order.id", "order.no", "order.totalAmount", "order.status", "order.createTime",
            "order.confirmTime", "order.completeTime", "order.month", "afterSale.no", "afterSale.status",
            "afterSale.type", "afterSale.createTime", "refund.id", "refund.amount", "refund.status",
            "refund.createTime", "refund.handleTime", "sku.id", "sku.code", "sku.name", "sku.stock",
            "sku.warnStock", "sku.status", "product.name", "product.status",
        }
        allowed_metrics = {
            "order_count", "order_amount_sum", "order_amount_avg", "refund_count", "refund_amount_sum",
            "after_sale_count", "customer_count", "sku_count", "low_stock_count",
        }
        allowed_operators = {"eq", "ne", "gt", "gte", "lt", "lte", "contains", "between", "in", "is_null", "not_null"}

        def validate(raw):
            text = re.sub(r"^```(?:json)?\s*|\s*```$", "", (raw or "").strip())
            plan = json.loads(text)
            if not isinstance(plan, dict) or not isinstance(plan.get("subject"), str):
                return None, "必须输出包含 subject 的 JSON 对象"
            plan = {key: value for key, value in plan.items() if key in allowed}
            if plan["subject"] not in allowed_subjects:
                return None, "subject 不在允许范围内"
            for key in ("fields", "groupBy", "metrics", "missingFields"):
                if key in plan and not isinstance(plan[key], list):
                    return None, f"{key} 必须是数组"
            if any(field not in allowed_fields for field in plan.get("fields", []) + plan.get("groupBy", [])):
                return None, "fields 或 groupBy 包含未登记字段"
            if any(metric not in allowed_metrics for metric in plan.get("metrics", [])):
                return None, "metrics 包含未登记指标"
            filters = plan.get("filters", [])
            if not isinstance(filters, list):
                return None, "filters 必须是数组"
            for item in filters:
                if not isinstance(item, dict) or item.get("field") not in allowed_fields or item.get("operator") not in allowed_operators:
                    return None, "filters 包含未登记字段或操作符"
            if plan.get("limit") is not None and (not isinstance(plan["limit"], int) or not 1 <= plan["limit"] <= 100):
                return None, "limit 必须是 1 到 100 的整数"
            if plan.get("fields") and not plan.get("metrics"):
                plan["detail"] = True
            if plan.get("subject") == "customer" and plan.get("fields") and not any(
                    str(item.get("field", "")).startswith("customer.") for item in plan.get("filters", [])
                    if isinstance(item, dict)) and not re.search(r"全部客户|所有客户|客户列表|全量客户", query or ""):
                plan["missingFields"] = ["customer.identity"]
            return plan, None

        try:
            repair = ""
            for attempt in range(2):
                response = self.llm.chat_completions(
                    base_prompt + repair, self.model, 0, self.top_p,
                    timeout=float(os.getenv("MODEL_QUERY_PLAN_TIMEOUT_SECONDS", "8")), retries=1)
                plan, error = validate(response)
                if plan:
                    return plan
                repair = f"\n上一次输出校验失败：{error}。请只输出修复后的 JSON，不要解释。"
                logger.info(f"只读 QueryPlan 第{attempt + 1}次校验失败：{error}")
            return None
        except Exception as exception:
            logger.info(f"模型未生成可用只读查询计划，回退既有路由: {exception}")
            return None

    def patch_model_read_query_plan(self, query, previous_plan):
        """用模型补充通用多轮修改，返回完整且受限的新计划。"""
        if not self.model_api_key or not isinstance(previous_plan, dict) or not re.search(
                r"只看|仅看|增加|加上|去掉|移除|排序|降序|升序|前\d+|最多\d+", query or ""):
            return None
        prompt = """你是只读 QueryPlan 修改器。根据上一轮计划和用户的新要求，输出完整 JSON 计划。
只能修改查询条件、字段、指标、分组、排序和 limit；必须保留上一轮未被用户修改的条件。
禁止 SQL、表名、JOIN、未登记字段和写操作。只输出 JSON，不要解释。
允许字段：customer.id, customer.name, customer.phone, customer.email, customer.status,
order.id, order.no, order.totalAmount, order.status, order.createTime, order.confirmTime, order.completeTime, order.month,
afterSale.no, afterSale.status, afterSale.type, afterSale.createTime,
refund.id, refund.amount, refund.status, refund.createTime, refund.handleTime,
sku.id, sku.code, sku.name, sku.stock, sku.warnStock, sku.status, product.name, product.status。
允许指标：order_count, order_amount_sum, order_amount_avg, refund_count, refund_amount_sum, after_sale_count, customer_count, sku_count, low_stock_count。
上一轮计划：""" + json.dumps(previous_plan, ensure_ascii=False) + "\n用户修改：" + query
        try:
            response = self.llm.chat_completions(
                prompt, self.model, 0, self.top_p,
                timeout=float(os.getenv("MODEL_QUERY_PLAN_TIMEOUT_SECONDS", "60")), retries=1)
            plan = json.loads(re.sub(r"^```(?:json)?\s*|\s*```$", "", (response or "").strip()))
            allowed_subjects = {"customer", "sale_order", "after_sale", "refund", "sku"}
            allowed_fields = {
                "customer.id", "customer.name", "customer.phone", "customer.email", "customer.status",
                "order.id", "order.no", "order.totalAmount", "order.status", "order.createTime",
                "order.confirmTime", "order.completeTime", "order.month", "afterSale.no", "afterSale.status",
                "afterSale.type", "afterSale.createTime", "refund.id", "refund.amount", "refund.status",
                "refund.createTime", "refund.handleTime", "sku.id", "sku.code", "sku.name", "sku.stock",
                "sku.warnStock", "sku.status", "product.name", "product.status",
            }
            allowed_metrics = {
                "order_count", "order_amount_sum", "order_amount_avg", "refund_count", "refund_amount_sum",
                "after_sale_count", "customer_count", "sku_count", "low_stock_count",
            }
            if plan.get("subject") not in allowed_subjects or any(
                    field not in allowed_fields for field in plan.get("fields", []) + plan.get("groupBy", [])):
                return None
            if any(metric not in allowed_metrics for metric in plan.get("metrics", [])):
                return None
            if not isinstance(plan.get("orderBy", []), list) or any(
                    not isinstance(item, dict) or item.get("field") not in allowed_fields | allowed_metrics
                    or item.get("direction") not in {"asc", "desc"}
                    for item in plan.get("orderBy", [])):
                return None
            if not isinstance(plan.get("filters", []), list) or any(
                    not isinstance(item, dict) or item.get("field") not in allowed_fields
                    or item.get("operator") not in {"eq", "ne", "gt", "gte", "lt", "lte", "contains", "between", "in", "is_null", "not_null"}
                    for item in plan.get("filters", [])):
                return None
            if not isinstance(plan.get("limit", 20), int) or not 1 <= plan.get("limit", 20) <= 100:
                return None
            return plan
        except Exception as exception:
            logger.info(f"模型未生成可用多轮修改计划，回退确定性 patch: {exception}")
            return None

    def _match_grouped_read_query_plan(self, query):
        """识别按月、按客户汇总及 Top N 订单统计。"""
        normalized = re.sub(r"\s+", "", query or "")
        year_match = re.search(r"(20\d{2})年", normalized)
        if "退款" in normalized and re.search(r"按(?:月|月份)|每月|每个月", normalized) and re.search(r"金额|总额|合计|数量|多少", normalized):
            metrics = []
            if re.search(r"数量|多少|笔数", normalized):
                metrics.append("refund_count")
            if re.search(r"金额|总额|合计", normalized):
                metrics.append("refund_amount_sum")
            filters = []
            if year_match:
                year = int(year_match.group(1))
                filters.append({
                    "field": "refund.createTime", "operator": "between",
                    "value": [f"{year}-01-01 00:00:00", f"{year + 1}-01-01 00:00:00"],
                })
            return {
                "subject": "refund", "detail": False, "fields": [], "filters": filters,
                "metrics": metrics, "groupBy": ["refund.month"],
                "orderBy": [{"field": "refund.month", "direction": "asc"}], "limit": 20,
            }
        if "订单" not in normalized and "销售" not in normalized:
            return None
        limit_match = re.search(r"(?:前|top)(\d+)", normalized, re.IGNORECASE)
        limit = min(int(limit_match.group(1)), 100) if limit_match else 20
        if re.search(r"按(?:月|月份)|每月|每个月", normalized) and re.search(r"销售金额|订单金额|销售额", normalized):
            filters = [{"field": "order.status", "operator": "eq", "value": "COMPLETED"}]
            if year_match:
                year = int(year_match.group(1))
                filters.append({
                    "field": "order.completeTime", "operator": "between",
                    "value": [f"{year}-01-01 00:00:00", f"{year + 1}-01-01 00:00:00"],
                })
            return {
                "subject": "sale_order", "detail": False, "fields": [], "filters": filters,
                "metrics": ["order_count", "order_amount_sum"], "groupBy": ["order.month"],
                "orderBy": [{"field": "order.month", "direction": "asc"}], "limit": limit,
            }
        if not re.search(r"每(?:位|个)客户|各客户|按客户", normalized):
            return None
        metrics = []
        if re.search(r"数量|多少|订单数|订单量", normalized):
            metrics.append("order_count")
        if re.search(r"金额|总额|合计", normalized):
            metrics.append("order_amount_sum")
        if not metrics:
            return None
        filters = []
        if year_match:
            year = int(year_match.group(1))
            filters.append({
                "field": "order.createTime", "operator": "between",
                "value": [f"{year}-01-01 00:00:00", f"{year + 1}-01-01 00:00:00"],
            })
        preferred_metric = "order_amount_sum" if "order_amount_sum" in metrics else "order_count"
        return {
            "subject": "sale_order", "detail": False, "fields": [], "filters": filters,
            "metrics": metrics, "groupBy": ["customer.name"],
            "orderBy": [{"field": preferred_metric, "direction": "desc"}], "limit": limit,
        }

    def _match_read_query_plan(self, query):
        """识别客户维度的订单、售后、退款统计，转换为受限的 QueryPlan。"""
        normalized = re.sub(r"\s+", "", query or "")
        subject_config = next((config for config in (
            {
                "subject": "refund", "keywords": ("退款记录", "退款"),
                "timeField": "refund.createTime", "countMetric": "refund_count",
                "amountMetric": "refund_amount_sum",
                "fields": ["refund.id", "customer.name", "order.no", "refund.amount", "refund.status", "refund.createTime"],
            },
            {
                "subject": "after_sale", "keywords": ("售后记录", "售后"),
                "timeField": "afterSale.createTime", "countMetric": "after_sale_count",
                "amountMetric": None,
                "fields": ["afterSale.no", "customer.name", "order.no", "afterSale.status", "afterSale.createTime"],
            },
            {
                "subject": "sale_order", "keywords": ("销售订单", "订单"),
                "timeField": "order.createTime", "countMetric": "order_count",
                "amountMetric": "order_amount_sum",
                "fields": ["order.no", "customer.name", "order.totalAmount", "order.status", "order.createTime"],
            },
        ) if any(keyword in normalized for keyword in config["keywords"])), None)
        if not subject_config:
            return None
        keywords = "|".join(subject_config["keywords"])
        name_match = re.search(
            rf"(?:今年|去年|本月|上月)([\u4e00-\u9fa5]{{2,20}}|1\d{{10}}|[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{{2,}})的(?:{keywords})|([\u4e00-\u9fa5]{{2,20}}|1\d{{10}}|[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{{2,}})的(?:{keywords})",
            normalized,
        )
        customer_value = next((item for item in name_match.groups() if item), None) if name_match else None
        if customer_value:
            customer_value = re.sub(r"(?:今年|去年|本月|上月|20\d{2}年)$", "", customer_value)
            customer_value = re.sub(r"^(?:请问|请帮我|帮我|查询|查看|查一下|查下|查|看下|看看)+", "", customer_value)
            customer_value = re.sub(r"^(?:今年|去年|本月|上月|20\d{2}年)", "", customer_value)
        if not customer_value:
            name_match = re.search(
                r"(?:今年|去年|本月|上月)([\u4e00-\u9fa5]{2,6})(?:一共|共有|有|的)|"
                r"([\u4e00-\u9fa5]{2,6})(?:今年|去年|本月|上月|20\d{2}年)的?(?:订单|一共|共有|有)",
                normalized,
            )
            if name_match:
                customer_value = next((item for item in name_match.groups() if item), None)
                customer_value = re.sub(r"^(?:帮我看看|我看看|看看|帮我查|查询|查看|查)", "", customer_value)
        if not customer_value:
            name_match = re.search(
                r"([\u4e00-\u9fa5]{2,6}|1\d{10}|[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,})(?:今年|去年|本月|上月|20\d{2}年)的?(?:销售订单|订单|退款记录|退款|售后记录|售后)",
                normalized,
            )
            if name_match:
                customer_value = name_match.group(1)
        if customer_value:
            customer_value = re.sub(r"(?:一共|共有)$", "", customer_value)
        if customer_value in ("他", "她", "他的", "她的", "某客户", "客户", "所有客户", "全部客户"):
            customer_value = None
        needs_count = bool(re.search(r"多少(?:张|笔|条)|几(?:张|笔|条)|数量|记录数", normalized))
        needs_amount = bool(re.search(r"金额|总额|合计", normalized))
        if needs_amount and not subject_config["amountMetric"]:
            return None
        has_customer_reference = bool(customer_value or re.search(r"(?:他|她)(?:的)?", normalized))
        detail_query = bool(has_customer_reference and re.search(r"订单|退款|售后", normalized))
        if not (needs_count or needs_amount or detail_query):
            return None
        if not customer_value and re.search(r"^(?:查询|查看|请问|帮我查)?订单(?:总金额|金额合计|总额)(?:是多少|有多少)?[？?]?$", normalized):
            return None

        metrics = []
        if needs_count:
            metrics.append(subject_config["countMetric"])
        if needs_amount:
            metrics.append(subject_config["amountMetric"])
        filters = []
        if customer_value:
            customer_field = "customer.phone" if re.fullmatch(r"1\d{10}", customer_value) \
                else "customer.email" if "@" in customer_value else "customer.name"
            filters.append({"field": customer_field, "operator": "eq", "value": customer_value})
        year_match = re.search(r"(20\d{2})年", normalized)
        if "今年" in normalized or year_match:
            year = int(year_match.group(1)) if year_match else date.today().year
            filters.append({
                "field": subject_config["timeField"], "operator": "between",
                "value": [f"{year}-01-01 00:00:00", f"{year + 1}-01-01 00:00:00"],
            })
        plan = {
            "subject": subject_config["subject"],
            "detail": detail_query or "信息" in normalized or "明细" in normalized,
            "fields": subject_config["fields"],
            "filters": filters,
            "metrics": metrics,
            "limit": 20,
        }
        if not customer_value:
            plan["missingFields"] = ["customer.identity"]
        return plan

    def _handle_read_query_plan(self, plan, query, task_id):
        missing_fields = plan.pop("missingFields", [])
        if missing_fields:
            self._set_task_type(query, task_id, TASK_TYPE_SINGLE, "等待补充客户条件")
            self.task_manager.update_task_recorder(
                task_id, TASK_STATUS_WAIT_INPUT,
                "请提供客户姓名、联系电话或邮箱，以继续查询。",
                graph_title="等待补充客户条件",
                result_payload={
                    "type": "pending_agent_query", "queryPlan": plan,
                    "missingFields": missing_fields, "originalQuery": query,
                })
            return
        tool = next((item for item in DigitalSalesCatalog().to_tools()
                     if item.operationId == "agent_query"), None)
        if tool is None:
            self.task_manager.update_task_recorder(
                task_id, TASK_STATUS_FINISH, TASK_SYS_OUTPUT_STOP + "只读查询工具不可用。",
                graph_title=GRAPH_TITLE_FAILURE)
            return
        self._set_task_type(query, task_id, TASK_TYPE_SINGLE, "正在执行受限只读查询")
        self.task_manager.update_task_recorder(
            task_id, TASK_STATUS_RUNNING, "正在查询业务数据...",
            curr_task_desc=query, curr_tool_id=tool.tool_id, curr_tool_param=plan)
        task = self.task_manager.get_task_by_id(task_id)
        result = self._process_single_api_invoke(query, task, tool, plan)
        if result.get("code") != TASK_SUCCESS_CODE:
            self.task_manager.update_task_recorder(
                task_id, TASK_STATUS_FINISH, TASK_SYS_OUTPUT_STOP + result["task_description"],
                graph_title=GRAPH_TITLE_FAILURE, result_payload=result.get("result_payload"))
            return
        payload = self._unwrap_result_data(result.get("result_payload"))
        fallback_summary = self._summarize_agent_query(payload)
        summary = self._model_summarize_agent_query(payload, fallback_summary)
        self.task_manager.update_task_recorder(
            task_id, TASK_STATUS_FINISH, summary,
            graph_title="查询完成", result_payload={**payload, "queryPlan": plan})

    def patch_read_query_plan(self, query, previous_plan):
        """对上一轮只读计划做有限的多轮修改，无法确定时返回 None。"""
        if not isinstance(previous_plan, dict) or not previous_plan.get("subject"):
            return None
        normalized = re.sub(r"\s+", "", query or "")
        if not normalized:
            return None
        if re.search(r"(?:查询|查看|帮我查|查一下|查下).*(?:订单|退款|售后)", normalized) \
                and not re.search(r"(?:他的|她的|换成|改成|换为|改为|再按|按月|按月份|每月)", normalized):
            return None
        plan = deepcopy(previous_plan)
        filters = [item for item in plan.get("filters", []) if isinstance(item, dict)]
        changed = False

        customer_match = re.search(r"(?:换成|改成|换为|改为)([\u4e00-\u9fa5]{2,6}|1\d{10}|[^\s，,。！？!?]+@[^\s，,。！？!?]+)", normalized)
        if customer_match:
            value = customer_match.group(1)
            field = "customer.phone" if re.fullmatch(r"1\d{10}", value) else "customer.email" if "@" in value else "customer.name"
            filters = [item for item in filters if not str(item.get("field", "")).startswith("customer.")]
            filters.append({"field": field, "operator": "eq", "value": value})
            changed = True

        subject_map = (
            ("refund", ("退款", "退款记录"), "refund.createTime", "refund_count", "refund_amount_sum",
             ["refund.id", "customer.name", "order.no", "refund.amount", "refund.status", "refund.createTime"]),
            ("after_sale", ("售后", "售后记录"), "afterSale.createTime", "after_sale_count", None,
             ["afterSale.no", "customer.name", "order.no", "afterSale.status", "afterSale.createTime"]),
            ("sale_order", ("订单", "销售订单"), "order.createTime", "order_count", "order_amount_sum",
             ["order.no", "customer.name", "order.totalAmount", "order.status", "order.createTime"]),
        )
        for subject, keywords, time_field, count_metric, amount_metric, fields in subject_map:
            if any(keyword in normalized for keyword in keywords) and subject != plan.get("subject"):
                plan["subject"] = subject
                plan["fields"] = fields
                plan["metrics"] = [count_metric] if not amount_metric else ([count_metric, amount_metric] if "金额" in normalized or "总额" in normalized else [count_metric])
                plan["filters"] = [
                    {**item, "field": time_field}
                    if str(item.get("field", "")).endswith(("createTime", "confirmTime", "completeTime"))
                    else item
                    for item in filters
                    if not str(item.get("field", "")).startswith(("order.", "refund.", "afterSale."))
                    or str(item.get("field", "")).endswith(("createTime", "confirmTime", "completeTime"))
                ]
                filters = plan["filters"]
                changed = True
                break

        if "按月" in normalized or "按月份" in normalized or "每月" in normalized:
            plan["detail"] = False
            month_subject = "refund" if "退款" in normalized else "sale_order"
            if month_subject == "refund":
                plan["groupBy"] = ["refund.month"]
                plan["orderBy"] = [{"field": "refund.month", "direction": "asc"}]
                plan["metrics"] = ["refund_count", "refund_amount_sum"] if "金额" in normalized or "总额" in normalized else ["refund_count"]
            else:
                plan["groupBy"] = ["order.month"]
                plan["orderBy"] = [{"field": "order.month", "direction": "asc"}]
                plan["subject"] = "sale_order"
                plan["metrics"] = ["order_count", "order_amount_sum"]
            plan["fields"] = []
            filters = [
                {**item, "field": "order.createTime"}
                if str(item.get("field", "")) in {"refund.createTime", "afterSale.createTime"}
                else item
                for item in filters
            ]
            changed = True

        if "今年" in normalized or "去年" in normalized:
            year = date.today().year if "今年" in normalized else date.today().year - 1
            time_field = {
                "refund": "refund.createTime", "after_sale": "afterSale.createTime",
            }.get(plan.get("subject"), "order.createTime")
            filters = [item for item in filters if item.get("field") != time_field]
            filters.append({"field": time_field, "operator": "between",
                            "value": [f"{year}-01-01 00:00:00", f"{year + 1}-01-01 00:00:00"]})
            changed = True

        if not changed:
            return None
        plan["filters"] = filters
        return plan

    def resume_read_query_plan(self, plan, query, task_id):
        """执行已由补参流程恢复的受限只读查询。"""
        self._handle_read_query_plan(plan, query, task_id)

    def _summarize_agent_query(self, payload):
        if not isinstance(payload, dict):
            return "查询完成。"
        summary = payload.get("summary") if isinstance(payload.get("summary"), dict) else {}
        rows = payload.get("rows") if isinstance(payload.get("rows"), list) else []
        if not rows and not summary:
            return "未找到符合条件的记录。"
        if rows and "customerName" in rows[0] and ("orderCount" in rows[0] or "orderAmount" in rows[0]):
            lines = []
            for row in rows:
                values = []
                if "orderCount" in row:
                    values.append(f"{row.get('orderCount', 0)} 张")
                if "orderAmount" in row:
                    values.append(f"{row.get('orderAmount', 0)} 元")
                lines.append(f"- {row.get('customerName')}：{'，'.join(values)}")
            return f"共返回 {len(rows)} 位客户的订单汇总：\n" + "\n".join(lines)
        if rows and "orderMonth" in rows[0]:
            lines = [
                f"- {row.get('orderMonth')}：{row.get('orderCount', 0)} 张，{row.get('orderAmount', 0)} 元"
                for row in rows
            ]
            return f"按月销售统计（已完成订单）：\n" + "\n".join(lines)
        if rows and "refundMonth" in rows[0]:
            has_count = any("refundCount" in row for row in rows)
            has_amount = any("refundAmount" in row for row in rows)
            total_count = sum(float(row.get("refundCount", 0) or 0) for row in rows)
            total_amount = sum(float(row.get("refundAmount", 0) or 0) for row in rows)
            lines = []
            for row in rows:
                values = []
                if has_count:
                    values.append(f"{row.get('refundCount', 0)} 条")
                if has_amount:
                    values.append(f"{row.get('refundAmount', 0)} 元")
                lines.append(f"- {row.get('refundMonth')}：{'，'.join(values)}")
            headline = []
            if has_count:
                headline.append(f"共 {int(total_count)} 条")
            if has_amount:
                headline.append(f"退款金额合计 {total_amount:g} 元")
            return "按月退款统计：" + "，".join(headline) + "。\n" + "\n".join(lines)
        parts = []
        if "orderCount" in summary:
            parts.append(f"订单共 {summary['orderCount']} 张")
        if "orderAmount" in summary:
            parts.append(f"金额合计 {summary['orderAmount']} 元")
        if "refundCount" in summary:
            parts.append(f"退款共 {summary['refundCount']} 条")
        if "refundAmount" in summary:
            parts.append(f"退款金额合计 {summary['refundAmount']} 元")
        if "afterSaleCount" in summary:
            parts.append(f"售后共 {summary['afterSaleCount']} 条")
        text = "，".join(parts) + "。" if parts else "查询完成。"
        meta = payload.get("queryMeta") if isinstance(payload.get("queryMeta"), dict) else {}
        if rows and meta.get("detail"):
            text += f"已返回 {len(rows)} 条明细。"
        return text

    def _model_summarize_agent_query(self, payload, fallback):
        """基于已查询事实生成统计摘要；明细和异常时保留确定性摘要。"""
        if not self.model_api_key or not isinstance(payload, dict):
            return fallback
        meta = payload.get("queryMeta") if isinstance(payload.get("queryMeta"), dict) else {}
        rows = payload.get("rows") if isinstance(payload.get("rows"), list) else []
        if not rows or meta.get("detail"):
            return fallback
        facts = json.dumps({"rows": rows[:100], "summary": payload.get("summary", {})}, ensure_ascii=False)
        prompt = (
            "你是数字销售分析助手。只能根据下面已查询到的事实生成简洁中文摘要。"
            "不得补造数据，不得输出JSON、SQL或查询过程。必须基于全部 rows 汇总，不能只使用第一行；"
            "指出总量、排序第一项和明显趋势；"
            "如果事实不足，只描述事实，不要猜测。\n查询事实：" + facts[:12000]
        )
        try:
            text = self.llm.chat_completions(
                prompt, self.model, 0, self.top_p,
                timeout=float(os.getenv("MODEL_SUMMARY_TIMEOUT_SECONDS", "20")), retries=1)
            text = (text or "").strip()
            if not text or text.startswith("{") or text.startswith("["):
                return fallback
            return text
        except Exception as exception:
            logger.info(f"模型结果总结失败，回退确定性摘要: {exception}")
            return fallback

    def _match_analytics_query(self, query):
        """识别确定性统计与计算，避免交给通用模型误选客户查询。"""
        normalized = re.sub(r"^[\s-]+", "", (query or "").strip())
        normalized = re.sub(r"\s+", "", normalized)
        normalized = re.sub(r"[？?。！!]$", "", normalized)
        rules = [
            ("customer_business_summary", ("统计今年每位客户的订单数量、订单金额、退款金额和净销售额", "统计每位客户的订单数量、订单金额、退款金额和净销售额")),
            ("net_sales_amount", ("订单金额减退款金额", "订单总额减退款总额", "净销售额")),
            ("customer_order_summary", ("每位客户的订单数量和金额", "每个客户的订单数量和金额", "各客户订单数量和金额")),
            ("order_status_summary", ("各订单状态分别有多少条", "各订单状态有多少条", "订单状态分别有多少")),
            ("monthly_order_amount_summary", ("按月份统计销售金额", "按月统计销售金额", "每月销售金额")),
            ("order_amount_avg", ("平均订单金额", "订单平均金额", "平均客单价", "客单价")),
            ("refund_amount_sum", ("退款总金额", "退款金额合计", "退款总额", "退款金额总计")),
            ("order_amount_sum", ("订单金额合计", "订单总金额", "销售金额合计", "销售额合计")),
            ("refund_count", ("退款记录数", "退款有多少条", "退款数量")),
            ("order_count", ("订单记录数", "订单有多少条", "订单数量", "订单数")),
            ("low_stock_count", ("库存低于或等于预警值的商品有多少个", "低库存商品有多少个", "低库存商品数量", "低库存有多少个")),
            ("customer_count", ("客户数量", "客户数", "有多少个客户")),
        ]
        for metric, phrases in rules:
            customer_business_query = metric == "customer_business_summary" and \
                re.search(r"(?:每位|每个|各)客户", normalized) and \
                (all(keyword in normalized for keyword in ("订单数量", "订单金额", "退款金额", "净销售额")) or
                 ("订单数量" in normalized and ("订单金额" in normalized or "金额" in normalized) and "退款" in normalized and
                  re.search(r"销售表现|销售情况|销售统计", normalized)))
            month_query = metric == "monthly_order_amount_summary" and re.search(
                r"按(?:月份|月)统计(?:20\d{2}年)?的?销售金额|(?:20\d{2}年)每月销售金额", normalized
            )
            if customer_business_query or month_query or any(phrase in normalized for phrase in phrases):
                filters = {}
                status = re.search(r"已完成|已取消|待确认|已确认", normalized)
                if status:
                    filters["status"] = {
                        "已完成": "COMPLETED", "已取消": "CANCELLED",
                        "待确认": "PENDING", "已确认": "CONFIRMED",
                    }[status.group()]
                customer_id = re.search(r"客户(?:ID)?\s*(\d+)", normalized)
                if customer_id:
                    filters["customerId"] = int(customer_id.group(1))
                year_month = re.search(r"(20\d{2})年(?:(\d{1,2})月)?", normalized)
                if year_month or "今年" in normalized:
                    year = int(year_month.group(1)) if year_month else date.today().year
                    month = int(year_month.group(2) or 1) if year_month else 1
                    begin = date(year, month, 1)
                    if year_month and year_month.group(2):
                        end = date(year + (month == 12), 1 if month == 12 else month + 1, 1)
                    else:
                        end = date(year + 1, 1, 1)
                    filters["beginTime"] = begin.isoformat() + " 00:00:00"
                    filters["endTime"] = end.isoformat() + " 00:00:00"
                return {"metric": metric, "filters": filters}
        return None

    def _handle_analytics_query(self, analytics, query, task_id):
        tool = next((item for item in DigitalSalesCatalog().to_tools()
                     if item.operationId == "analytics_query"), None)
        if tool is None:
            self.task_manager.update_task_recorder(
                task_id, TASK_STATUS_FINISH, TASK_SYS_OUTPUT_STOP + "统计查询工具不可用。",
                graph_title=GRAPH_TITLE_FAILURE
            )
            return
        params = {"metric": analytics["metric"], "filters": analytics.get("filters") or {}}
        self._set_task_type(query, task_id, TASK_TYPE_SINGLE, "正在统计业务数据")
        self.task_manager.update_task_recorder(
            task_id, TASK_STATUS_RUNNING, "正在统计业务数据...",
            curr_task_desc=query, curr_tool_id=tool.tool_id, curr_tool_param=params
        )
        task = self.task_manager.get_task_by_id(task_id)
        result = self._process_single_api_invoke(query, task, tool, params)
        self.task_manager.update_task_recorder(
            task_id, TASK_STATUS_FINISH,
            result["result"] if result.get("code") == TASK_SUCCESS_CODE
            else TASK_SYS_OUTPUT_STOP + result["task_description"],
            graph_title="统计完成" if result.get("code") == TASK_SUCCESS_CODE else GRAPH_TITLE_FAILURE,
            result_payload=result.get("result_payload")
        )

    def _handle_low_stock_query(self, query, task_id):
        """读取看板中的低库存列表，直接返回库存与预警值。"""
        tool = next((item for item in DigitalSalesCatalog().to_tools()
                     if item.operationId == "dashboard_overview"), None)
        if tool is None:
            self.task_manager.update_task_recorder(
                task_id, TASK_STATUS_FINISH, TASK_SYS_OUTPUT_STOP + "低库存查询工具不可用。",
                graph_title=GRAPH_TITLE_FAILURE
            )
            return
        self._set_task_type(query, task_id, TASK_TYPE_SINGLE, "正在查询低库存商品")
        task = self.task_manager.get_task_by_id(task_id)
        success, normalized = normalize_response(self.tool_use_hub.tool_use(tool, {}))
        if not success:
            message = normalized.get("errorMessage") or "低库存查询失败"
            self.task_manager.update_task_recorder(
                task_id, TASK_STATUS_FINISH, TASK_SYS_OUTPUT_STOP + message,
                graph_title=GRAPH_TITLE_FAILURE, result_payload=normalized
            )
            return
        data = self._unwrap_result_data(normalized.get("data"))
        rows = data.get("lowStockList", []) if isinstance(data, dict) else []
        rows = [row for row in rows if isinstance(row, dict)]
        lines = []
        for row in rows:
            name = row.get("productName") or "未命名商品"
            sku = row.get("skuName") or row.get("skuCode") or "未指定规格"
            lines.append(f"- {name}（{sku}）：当前库存 {row.get('stock', 0)}，预警值 {row.get('warnStock', 0)}")
        text = f"共找到 {len(rows)} 个库存达到或低于预警值的商品。"
        if lines:
            text += "\n" + "\n".join(lines)
        else:
            text = "当前没有库存达到或低于预警值的商品。"
        self.task_manager.update_task_recorder(
            task_id, TASK_STATUS_FINISH, text,
            graph_title="查询完成", result_payload={"total": len(rows), "rows": rows}
        )

    def _match_multi_customer_read_query(self, query):
        """识别同一客户一次查询多个只读业务，避免交给单任务规划器丢失后半段。"""
        business_map = {
            "销售订单": "订单", "订单": "订单",
            "售后记录": "售后记录", "售后": "售后记录",
            "退款记录": "退款记录", "退款": "退款记录",
        }
        subject_matches = re.findall(
            r"(?:查询|查|看下|查看|看看|再看下|再看看|帮我看下)?(?:客户(?:ID\s*)?)?([\u4e00-\u9fa5]{2,6}?|\d+)(?=的(?:销售订单|订单|售后记录|售后|退款记录|退款))",
            query
        )
        paired = re.findall(r"([\u4e00-\u9fa5]{2,6})的(销售订单|订单|售后记录|售后|退款记录|退款)", query)
        if len(paired) >= 2:
            paired = [(re.sub(r"^(?:查询|查看|看看|查)", "", name.lstrip("和及、")), business) for name, business in paired]
            return [
                (f"查询客户{name}的{business_map[business]}", business_map[business])
                for name, business in paired
            ]
        businesses = [business_map[item] for item in re.findall(r"销售订单|订单|售后记录|售后|退款记录|退款", query)]
        if len(set(businesses)) < 2 or not subject_matches:
            return None
        customer = subject_matches[0]
        customer_ref = f"客户ID {customer}" if customer.isdigit() else f"客户{customer}"
        return [(f"查询{customer_ref}的{business}", business) for business in dict.fromkeys(businesses)]

    def _handle_multi_customer_read(self, queries, original_query, task_id):
        """执行多个只读查询并分别保留结果，写操作仍走原有确认流程。"""
        self._set_task_type(original_query, task_id, TASK_TYPE_APIS, "正在查询多个业务数据")
        task = self.task_manager.get_task_by_id(task_id)
        results = []
        failures = []
        sub_queries = []
        for clarified_query, _ in queries:
            matched = self._match_clarified_customer_query(clarified_query)
            if not matched:
                failures.append({"query": clarified_query, "message": "未识别查询业务"})
                continue
            tool, params = self._resolve_customer_order_query(matched, task_id, original_query)
            if tool is None:
                latest_task = self.task_manager.get_task_by_id(task_id)
                if latest_task and latest_task.status == TASK_STATUS_WAIT_INPUT:
                    return
                failures.append({"query": clarified_query, "message": "未找到对应客户或业务工具"})
                continue
            result = self._process_single_api_invoke(clarified_query, task, tool, params)
            if result.get("code") != TASK_SUCCESS_CODE:
                failures.append({
                    "query": clarified_query,
                    "business": tool.name_for_human,
                    "message": result["task_description"],
                })
                continue
            payload = result.get("result_payload") or {}
            query_status = payload.get("queryStatus", "has_data") if isinstance(payload, dict) else "has_data"
            results.append({
                "query": clarified_query,
                "business": tool.name_for_human,
                "status": query_status,
                "text": result["result"],
                "payload": payload,
            })
            sub_queries.append({
                "business": tool.operationId,
                "status": query_status,
                "total": payload.get("total", 0) if isinstance(payload, dict) else 0,
                "rows": payload.get("rows", []) if isinstance(payload, dict) else [],
            })
        if not results and failures:
            self.task_manager.update_task_recorder(
                task_id, TASK_STATUS_FINISH,
                TASK_SYS_OUTPUT_STOP + "所有查询均未成功：" + "；".join(item["message"] for item in failures),
                graph_title=GRAPH_TITLE_FAILURE,
                result_payload={"queryStatus": "failed", "subQueries": sub_queries, "failures": failures}
            )
            return
        if failures:
            overall_status = "partial_success"
        elif any(item["status"] == "has_data" for item in results):
            overall_status = "has_data"
        else:
            overall_status = "no_data"
        text_parts = [item["text"] for item in results]
        if failures:
            text_parts.append("以下查询未完成：" + "；".join(
                f"{item.get('business', item['query'])}：{item['message']}" for item in failures
            ))
        payload = {
            "queryStatus": overall_status,
            "subQueries": sub_queries,
            "failures": failures,
        }
        self.task_manager.update_task_recorder(
            task_id, TASK_STATUS_FINISH, "\n\n".join(text_parts), graph_title="查询完成",
            result_payload=payload
        )

    def _resolve_customer_order_query(self, clarified_query, task_id, query):
        """先解析客户姓名，避免同名客户直接被业务接口模糊匹配。"""
        order_tool, params = clarified_query
        if order_tool.operationId == "customer_list":
            return order_tool, params
        if "customerId" in params:
            return order_tool, params
        customer_name = params.get("customerName")
        customer_phone = params.get("customerPhone")
        customer_email = params.get("customerEmail")
        from utils.digital_sales_catalog import DigitalSalesCatalog
        customer_tool = next((item for item in DigitalSalesCatalog().to_tools()
                              if item.operationId == "customer_list"), None)
        if customer_tool is None:
            return order_tool, params
        lookup_params = {"pageNum": 1, "pageSize": 100}
        if customer_name:
            lookup_params["customerName"] = customer_name
        elif customer_phone:
            lookup_params["phone"] = customer_phone
        response = self.tool_use_hub.tool_use(customer_tool, lookup_params)
        success, normalized = normalize_response(response)
        rows = normalized.get("rows", []) if success else []
        if not rows and success:
            data = normalized.get("data") or {}
            rows = data.get("rows", []) if isinstance(data, dict) else []
            if not rows and isinstance(data, dict) and isinstance(data.get("data"), dict):
                rows = data["data"].get("rows", [])
        # 数据中的客户可能带有“演示客户-”等前缀，用户通常只输入姓名后缀。
        # 接口未返回直接结果时，再取一页客户候选做包含匹配，避免简称被误判为不存在。
        if not rows and success and (customer_name or customer_email):
            fallback = self.tool_use_hub.tool_use(customer_tool, {"pageNum": 1, "pageSize": 100})
            fallback_success, fallback_data = normalize_response(fallback)
            if fallback_success:
                rows = fallback_data.get("rows", [])
                if not rows and isinstance(fallback_data.get("data"), dict):
                    rows = fallback_data["data"].get("rows", [])
                if customer_email:
                    rows = [row for row in rows if customer_email.lower() == str(row.get("email") or "").lower()]
                else:
                    rows = [row for row in rows if customer_name in str(row.get("customerName", ""))]
        if len(rows) == 0:
            self.task_manager.update_task_recorder(
                task_id, TASK_STATUS_FINISH, f"未找到对应客户，请核对客户姓名、联系电话或邮箱。",
                graph_title=GRAPH_TITLE_FAILURE
            )
            return None, None
        if len(rows) > 1:
            options = []
            for row in rows:
                customer_id = row.get("customerId")
                phone = str(row.get("phone") or "")
                masked_phone = phone[:3] + "****" + phone[-4:] if len(phone) >= 7 else phone
                options.append({
                    "label": f"{row.get('customerName', customer_name)}，手机号 {masked_phone}，客户ID {customer_id}",
                    "value": str(customer_id)
                })
            self.task_manager.update_task_recorder(
                task_id, TASK_STATUS_WAIT_INPUT, f"找到多位名为“{customer_name}”的客户，请选择要查询的客户。",
                graph_title="等待选择客户",
                result_payload={
                    "type": "clarification", "reason": "entity_ambiguous",
                    "entityType": "customer", "entityName": customer_name,
                    "question": f"找到多位名为“{customer_name}”的客户，请选择：",
                    "options": options, "allowFreeText": True
                }
            )
            return None, None
        resolved_customer_name = rows[0].get("customerName") or customer_name
        params = {"pageNum": 1, "pageSize": 20, "customerName": resolved_customer_name}
        # 售后、退款接口仅支持按客户姓名查询；订单接口支持客户 ID，保留该条件以避免同名误匹配。
        if order_tool.operationId == "order_list":
            params["customerId"] = rows[0].get("customerId")
        return order_tool, params

    def _match_fast_stock_tool(self, query):
        """匹配低歧义的库存调整指令，无法确定时返回 None 交给模型处理。"""
        if not re.search(r"SKU\s*\d+", query, re.IGNORECASE):
            return None
        if not re.search(r"\d+\s*(件|个|台|套)", query):
            return None
        operation = "stock_inbound" if re.search(r"入库|入仓|增加库存", query) else None
        if not operation and re.search(r"出库|出仓|减少库存", query):
            operation = "stock_outbound"
        if not operation:
            return None
        from utils.digital_sales_catalog import DigitalSalesCatalog
        return next((tool for tool in DigitalSalesCatalog().to_tools() if tool.operationId == operation), None)

    def _match_fast_order_action(self, query):
        match = re.search(r"(?:订单|单据)\s*(?:ID\s*)?(\d+)", query, re.IGNORECASE)
        if not match:
            return None
        if re.search(r"确认销售|确认订单|确认", query):
            operation = "order_confirm"
        elif re.search(r"取消订单|取消", query):
            operation = "order_cancel"
        elif re.search(r"完成订单|完成", query):
            operation = "order_complete"
        else:
            return None
        from utils.digital_sales_catalog import DigitalSalesCatalog
        tool = next((item for item in DigitalSalesCatalog().to_tools() if item.operationId == operation), None)
        return (tool, {"orderId": int(match.group(1))}) if tool else None

    def _match_fast_service_action(self, query):
        ticket = re.search(r"售后(?:单|工单)?\s*(?:ID\s*)?(\d+)", query, re.IGNORECASE)
        refund = re.search(r"退款(?:单|记录)?\s*(?:ID\s*)?(\d+)", query, re.IGNORECASE)
        if "创建售后" in query or "新建售后" in query:
            item = re.search(r"(?:订单明细|明细)(?:ID)?\s*[:：]?\s*(\d+)", query)
            quantity = re.search(r"(?:数量|申请数量)\s*[:：]?\s*(\d+)", query)
            if not (item and quantity):
                return None
            ticket_type = "RETURN" if "退货" in query else "REPAIR"
            fault = re.search(r"(?:问题描述|原因)\s*[:：]?\s*(.+)$", query)
            params = {"orderItemId": int(item.group(1)), "ticketType": ticket_type,
                      "applyQuantity": int(quantity.group(1)),
                      "faultDesc": fault.group(1).strip() if fault else "Agent售后测试"}
            operation = "aftersale_create"
        elif "退货检查" in query:
            if not ticket:
                return None
            received = re.search(r"(?:实收|收到)数量\s*[:：]?\s*(\d+)", query)
            if not received:
                return None
            params = {"ticketId": int(ticket.group(1)), "receivedQuantity": int(received.group(1)),
                      "handlingResult": "GOOD" if "完好" in query else "DAMAGED",
                      "remark": "Agent阶段退款回归测试"}
            operation = "aftersale_return_check"
        elif ticket:
            if "关闭" in query:
                operation = "aftersale_close"
            elif "完成" in query:
                operation = "aftersale_complete"
            elif "处理" in query:
                operation = "aftersale_handle"
            else:
                return None
            params = {"ticketId": int(ticket.group(1))}
            reason = re.search(r"(?:原因|处理结果)\s*[:：]?\s*(.+)$", query)
            if reason and operation in ("aftersale_close", "aftersale_complete", "aftersale_handle"):
                params["handleResult"] = reason.group(1).strip()
        elif refund and "审核" in query:
            operation = "refund_audit"
            params = {"refundId": int(refund.group(1))}
            amount = re.search(r"(?:金额|退款金额)\s*[:：]?\s*(\d+(?:\.\d{1,2})?)", query)
            if amount:
                params["refundAmount"] = float(amount.group(1))
        else:
            return None
        from utils.digital_sales_catalog import DigitalSalesCatalog
        tool = next((item for item in DigitalSalesCatalog().to_tools() if item.operationId == operation), None)
        return (tool, params) if tool else None

    def _extract_fast_stock_params(self, query, tool):
        sku_match = re.search(r"SKU\s*(\d+)", query, re.IGNORECASE)
        quantity_match = re.search(r"(\d+)\s*(件|个|台|套)", query)
        return {
            "skuId": int(sku_match.group(1)),
            "quantity": int(quantity_match.group(1)),
            "requestNo": f"agent-{uuid.uuid4().hex}",
            "remark": f"Agent操作：{query}" if tool.operationId == "stock_outbound" else ""
        }
