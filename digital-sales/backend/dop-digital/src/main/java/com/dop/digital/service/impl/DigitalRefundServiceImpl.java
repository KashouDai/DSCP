package com.dop.digital.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.dop.common.exception.ServiceException;
import com.dop.common.utils.StringUtils;
import com.dop.digital.domain.DigitalRefundRecord;
import com.dop.digital.domain.DigitalSaleOrderItem;
import com.dop.digital.mapper.DigitalRefundRecordMapper;
import com.dop.digital.mapper.DigitalSaleOrderItemMapper;
import com.dop.digital.service.IDigitalRefundService;

/**
 * 退款记录 服务层实现
 *
 * @author dop
 */
@Service
public class DigitalRefundServiceImpl implements IDigitalRefundService
{
    private static final String STATUS_PENDING = "PENDING";

    private static final String STATUS_REFUNDED = "REFUNDED";

    private static final String STATUS_CANCELLED = "CANCELLED";

    @Autowired
    private DigitalRefundRecordMapper refundMapper;

    @Autowired
    private DigitalSaleOrderItemMapper orderItemMapper;

    @Override
    public List<DigitalRefundRecord> selectDigitalRefundRecordList(DigitalRefundRecord refund)
    {
        return refundMapper.selectDigitalRefundRecordList(refund);
    }

    @Override
    public DigitalRefundRecord selectDigitalRefundRecordById(Long refundId)
    {
        if (StringUtils.isNull(refundId))
        {
            return null;
        }
        return refundMapper.selectDigitalRefundRecordById(refundId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DigitalRefundRecord auditDigitalRefund(Long refundId, DigitalRefundRecord request,
            Long handlerId, String operator)
    {
        validateAuditRequest(refundId, request);
        DigitalRefundRecord locked = requireRefundForUpdate(refundId);
        if (STATUS_REFUNDED.equals(locked.getStatus()))
        {
            return loadRefund(refundId);
        }
        if (!STATUS_PENDING.equals(locked.getStatus()))
        {
            throw new ServiceException("当前退款不能审核");
        }

        DigitalRefundRecord refund = loadRefund(refundId);
        if (StringUtils.isNull(refund))
        {
            throw new ServiceException("退款记录不存在");
        }
        DigitalSaleOrderItem item =
                orderItemMapper.selectDigitalSaleOrderItemByIdForUpdate(refund.getOrderItemId());
        if (StringUtils.isNull(item))
        {
            throw new ServiceException("订单明细不存在");
        }

        BigDecimal amount = normalizeAmount(request.getRefundAmount());
        BigDecimal maxRefundAmount = calculateMaxRefundAmount(refund, item);
        if (amount.compareTo(maxRefundAmount) > 0)
        {
            throw new ServiceException("退款金额超过可退上限");
        }

        if (refundMapper.auditDigitalRefund(refundId, amount, handlerId, operator) != 1)
        {
            DigitalRefundRecord current = refundMapper.selectDigitalRefundRecordByIdForUpdate(refundId);
            if (StringUtils.isNotNull(current) && STATUS_REFUNDED.equals(current.getStatus()))
            {
                return loadRefund(refundId);
            }
            throw new ServiceException("退款状态已变化，请刷新后重试");
        }
        if (orderItemMapper.increaseRefundedAmount(refund.getOrderItemId(), amount, operator) != 1)
        {
            throw new ServiceException("累计退款金额失败");
        }
        return loadRefund(refundId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DigitalRefundRecord cancelDigitalRefund(Long refundId, DigitalRefundRecord request,
            Long handlerId, String operator)
    {
        if (StringUtils.isNull(request) || StringUtils.isEmpty(request.getRemark()))
        {
            throw new ServiceException("取消原因不能为空");
        }
        DigitalRefundRecord locked = requireRefundForUpdate(refundId);
        if (STATUS_CANCELLED.equals(locked.getStatus()))
        {
            return loadRefund(refundId);
        }
        if (!STATUS_PENDING.equals(locked.getStatus()))
        {
            throw new ServiceException("当前退款不能取消");
        }
        if (refundMapper.cancelDigitalRefund(refundId, request.getRemark(), handlerId, operator) != 1)
        {
            DigitalRefundRecord current = refundMapper.selectDigitalRefundRecordByIdForUpdate(refundId);
            if (StringUtils.isNotNull(current) && STATUS_CANCELLED.equals(current.getStatus()))
            {
                return loadRefund(refundId);
            }
            throw new ServiceException("退款状态已变化，请刷新后重试");
        }
        return loadRefund(refundId);
    }

    private void validateAuditRequest(Long refundId, DigitalRefundRecord request)
    {
        if (StringUtils.isNull(refundId))
        {
            throw new ServiceException("退款记录不能为空");
        }
        if (StringUtils.isNull(request) || StringUtils.isNull(request.getRefundAmount()))
        {
            throw new ServiceException("退款金额不能为空");
        }
    }

    private DigitalRefundRecord requireRefundForUpdate(Long refundId)
    {
        if (StringUtils.isNull(refundId))
        {
            throw new ServiceException("退款记录不能为空");
        }
        DigitalRefundRecord refund = refundMapper.selectDigitalRefundRecordByIdForUpdate(refundId);
        if (StringUtils.isNull(refund))
        {
            throw new ServiceException("退款记录不存在");
        }
        return refund;
    }

    private BigDecimal normalizeAmount(BigDecimal amount)
    {
        if (amount.compareTo(BigDecimal.ZERO) <= 0)
        {
            throw new ServiceException("退款金额必须大于0");
        }
        if (amount.scale() > 2)
        {
            throw new ServiceException("退款金额最多保留两位小数");
        }
        return amount.setScale(2, RoundingMode.UNNECESSARY);
    }

    private BigDecimal calculateMaxRefundAmount(DigitalRefundRecord refund, DigitalSaleOrderItem item)
    {
        if (StringUtils.isNull(refund.getSalePrice()) || StringUtils.isNull(refund.getReceivedQuantity()))
        {
            throw new ServiceException("退款商品信息不完整");
        }
        BigDecimal salePrice = refund.getSalePrice();
        BigDecimal maxByReceived = salePrice.multiply(
                BigDecimal.valueOf(refund.getReceivedQuantity()));
        BigDecimal soldAmount = salePrice.multiply(
                BigDecimal.valueOf(defaultZero(item.getQuantity())));
        BigDecimal refundedAmount = StringUtils.isNull(item.getRefundedAmount())
                ? BigDecimal.ZERO : item.getRefundedAmount();
        BigDecimal remainingAmount = soldAmount.subtract(refundedAmount);
        if (remainingAmount.compareTo(BigDecimal.ZERO) < 0)
        {
            remainingAmount = BigDecimal.ZERO;
        }
        return maxByReceived.compareTo(remainingAmount) <= 0 ? maxByReceived : remainingAmount;
    }

    private int defaultZero(Integer value)
    {
        return StringUtils.isNull(value) ? 0 : value;
    }

    private DigitalRefundRecord loadRefund(Long refundId)
    {
        return refundMapper.selectDigitalRefundRecordById(refundId);
    }
}
