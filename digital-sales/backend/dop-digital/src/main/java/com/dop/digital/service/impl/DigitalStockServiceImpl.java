package com.dop.digital.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.dop.common.exception.ServiceException;
import com.dop.common.utils.StringUtils;
import com.dop.digital.domain.DigitalProductSku;
import com.dop.digital.domain.DigitalStockInboundRequest;
import com.dop.digital.domain.DigitalStockRecord;
import com.dop.digital.mapper.DigitalProductSkuMapper;
import com.dop.digital.mapper.DigitalStockRecordMapper;
import com.dop.digital.service.IDigitalStockService;

/**
 * 库存 服务层实现
 *
 * @author dop
 */
@Service
public class DigitalStockServiceImpl implements IDigitalStockService
{
    /** 手工入库流水类型 */
    private static final String RECORD_TYPE_IN = "IN";

    /** 手工入库业务类型 */
    private static final String BIZ_TYPE_MANUAL_IN = "MANUAL_IN";

    private static final String RECORD_TYPE_OUT = "OUT";

    private static final String BIZ_TYPE_MANUAL_OUT = "MANUAL_OUT";

    /** 退货入库流水类型 */
    private static final String RECORD_TYPE_RETURN_IN = "RETURN_IN";

    /** 退货入库业务类型 */
    private static final String BIZ_TYPE_RETURN = "RETURN";

    @Autowired
    private DigitalProductSkuMapper skuMapper;

    @Autowired
    private DigitalStockRecordMapper stockRecordMapper;

    @Override
    public List<DigitalProductSku> selectStockList(DigitalProductSku sku)
    {
        return skuMapper.selectDigitalProductSkuList(sku);
    }

    @Override
    public List<DigitalStockRecord> selectDigitalStockRecordList(DigitalStockRecord record)
    {
        return stockRecordMapper.selectDigitalStockRecordList(record);
    }

    @Override
    public DigitalStockRecord selectDigitalStockRecordByRequestNo(String requestNo)
    {
        return stockRecordMapper.selectDigitalStockRecordByRequestNo(requestNo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DigitalStockRecord inbound(DigitalStockInboundRequest request, String operator)
    {
        validateInboundRequest(request);

        DigitalStockRecord existing = stockRecordMapper.selectDigitalStockRecordByRequestNo(request.getRequestNo());
        if (StringUtils.isNotNull(existing))
        {
            return existing;
        }

        DigitalProductSku sku = skuMapper.selectDigitalProductSkuById(request.getSkuId());
        if (StringUtils.isNull(sku))
        {
            throw new ServiceException("商品规格不存在或已删除");
        }

        int rows = skuMapper.increaseStock(request.getSkuId(), request.getQuantity(), operator);
        if (rows != 1)
        {
            throw new ServiceException("库存更新失败");
        }

        DigitalProductSku updatedSku = skuMapper.selectDigitalProductSkuById(request.getSkuId());
        if (StringUtils.isNull(updatedSku) || StringUtils.isNull(updatedSku.getStock()))
        {
            throw new ServiceException("读取库存失败");
        }

        DigitalStockRecord record = new DigitalStockRecord();
        record.setSkuId(request.getSkuId());
        record.setRecordType(RECORD_TYPE_IN);
        record.setChangeDirection(1);
        record.setQuantity(request.getQuantity());
        record.setBeforeStock(updatedSku.getStock() - request.getQuantity());
        record.setAfterStock(updatedSku.getStock());
        record.setBizType(BIZ_TYPE_MANUAL_IN);
        record.setRequestNo(request.getRequestNo());
        record.setCreateBy(operator);
        record.setRemark(request.getRemark());
        stockRecordMapper.insertDigitalStockRecord(record);
        return record;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DigitalStockRecord outbound(DigitalStockInboundRequest request, String operator)
    {
        validateInboundRequest(request);
        if (StringUtils.isEmpty(request.getRemark()))
        {
            throw new ServiceException("减少库存必须填写原因");
        }
        DigitalStockRecord existing = stockRecordMapper.selectDigitalStockRecordByRequestNo(request.getRequestNo());
        if (StringUtils.isNotNull(existing))
        {
            return existing;
        }
        if (StringUtils.isNull(skuMapper.selectDigitalProductSkuById(request.getSkuId())))
        {
            throw new ServiceException("商品规格不存在或已删除");
        }
        if (skuMapper.decreaseStock(request.getSkuId(), request.getQuantity(), operator) != 1)
        {
            throw new ServiceException("库存不足，无法减少");
        }
        DigitalProductSku updatedSku = skuMapper.selectDigitalProductSkuById(request.getSkuId());
        if (StringUtils.isNull(updatedSku) || StringUtils.isNull(updatedSku.getStock()))
        {
            throw new ServiceException("读取库存失败");
        }
        DigitalStockRecord record = new DigitalStockRecord();
        record.setSkuId(request.getSkuId());
        record.setRecordType(RECORD_TYPE_OUT);
        record.setChangeDirection(-1);
        record.setQuantity(request.getQuantity());
        record.setBeforeStock(updatedSku.getStock() + request.getQuantity());
        record.setAfterStock(updatedSku.getStock());
        record.setBizType(BIZ_TYPE_MANUAL_OUT);
        record.setRequestNo(request.getRequestNo());
        record.setCreateBy(operator);
        record.setRemark(request.getRemark());
        if (stockRecordMapper.insertDigitalStockRecord(record) != 1)
        {
            throw new ServiceException("记录库存流水失败");
        }
        return record;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DigitalStockRecord returnInbound(Long skuId, Integer quantity, Long ticketId,
            String operator, String remark)
    {
        if (StringUtils.isNull(skuId) || StringUtils.isNull(ticketId))
        {
            throw new ServiceException("退货入库信息不完整");
        }
        if (StringUtils.isNull(quantity) || quantity <= 0)
        {
            throw new ServiceException("退货入库数量必须大于0");
        }

        DigitalProductSku sku = skuMapper.selectDigitalProductSkuById(skuId);
        if (StringUtils.isNull(sku))
        {
            throw new ServiceException("商品规格不存在或已删除");
        }
        if (skuMapper.increaseStock(skuId, quantity, operator) != 1)
        {
            throw new ServiceException("库存更新失败");
        }

        DigitalProductSku updatedSku = skuMapper.selectDigitalProductSkuById(skuId);
        if (StringUtils.isNull(updatedSku) || StringUtils.isNull(updatedSku.getStock()))
        {
            throw new ServiceException("读取库存失败");
        }

        DigitalStockRecord record = new DigitalStockRecord();
        record.setSkuId(skuId);
        record.setRecordType(RECORD_TYPE_RETURN_IN);
        record.setChangeDirection(1);
        record.setQuantity(quantity);
        record.setBeforeStock(updatedSku.getStock() - quantity);
        record.setAfterStock(updatedSku.getStock());
        record.setBizType(BIZ_TYPE_RETURN);
        record.setBizId(ticketId);
        record.setCreateBy(operator);
        record.setRemark(remark);
        if (stockRecordMapper.insertDigitalStockRecord(record) != 1)
        {
            throw new ServiceException("记录退货入库流水失败");
        }
        return record;
    }

    private void validateInboundRequest(DigitalStockInboundRequest request)
    {
        if (StringUtils.isNull(request) || StringUtils.isNull(request.getSkuId()))
        {
            throw new ServiceException("规格不能为空");
        }
        if (StringUtils.isNull(request.getQuantity()) || request.getQuantity() <= 0)
        {
            throw new ServiceException("入库数量必须大于0");
        }
        if (StringUtils.isEmpty(request.getRequestNo()))
        {
            throw new ServiceException("入库请求号不能为空");
        }
    }
}
