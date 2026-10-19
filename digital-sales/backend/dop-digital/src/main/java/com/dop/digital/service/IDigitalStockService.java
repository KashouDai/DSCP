package com.dop.digital.service;

import java.util.List;
import com.dop.digital.domain.DigitalProductSku;
import com.dop.digital.domain.DigitalStockInboundRequest;
import com.dop.digital.domain.DigitalStockRecord;

/**
 * 库存 服务层
 *
 * @author dop
 */
public interface IDigitalStockService
{
    /**
     * 查询当前库存列表
     *
     * @param sku 商品规格
     * @return 库存列表
     */
    public List<DigitalProductSku> selectStockList(DigitalProductSku sku);

    /**
     * 查询库存流水列表
     *
     * @param record 库存流水
     * @return 库存流水集合
     */
    public List<DigitalStockRecord> selectDigitalStockRecordList(DigitalStockRecord record);

    /**
     * 根据请求号查询库存流水
     *
     * @param requestNo 请求号
     * @return 库存流水
     */
    public DigitalStockRecord selectDigitalStockRecordByRequestNo(String requestNo);

    /**
     * 手工入库
     *
     * @param request 入库请求
     * @param operator 操作人
     * @return 入库流水
     */
    public DigitalStockRecord inbound(DigitalStockInboundRequest request, String operator);

    public DigitalStockRecord outbound(DigitalStockInboundRequest request, String operator);

    /**
     * 退货入库
     *
     * @param skuId 规格ID
     * @param quantity 入库数量
     * @param ticketId 售后工单ID
     * @param operator 操作人
     * @param remark 备注
     * @return 入库流水
     */
    public DigitalStockRecord returnInbound(Long skuId, Integer quantity, Long ticketId,
            String operator, String remark);
}
