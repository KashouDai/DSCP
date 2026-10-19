package com.dop.digital.service;

import java.util.List;
import com.dop.digital.domain.DigitalRefundRecord;

/**
 * 退款记录 服务层
 *
 * @author dop
 */
public interface IDigitalRefundService
{
    /**
     * 查询退款记录列表
     *
     * @param refund 退款记录
     * @return 退款记录集合
     */
    public List<DigitalRefundRecord> selectDigitalRefundRecordList(DigitalRefundRecord refund);

    /**
     * 根据退款ID查询退款记录
     *
     * @param refundId 退款ID
     * @return 退款记录
     */
    public DigitalRefundRecord selectDigitalRefundRecordById(Long refundId);

    /**
     * 审核退款
     *
     * @param refundId 退款ID
     * @param refund 退款请求
     * @param handlerId 处理人ID
     * @param operator 操作人
     * @return 退款记录
     */
    public DigitalRefundRecord auditDigitalRefund(Long refundId, DigitalRefundRecord refund,
            Long handlerId, String operator);

    /**
     * 取消退款
     *
     * @param refundId 退款ID
     * @param refund 退款请求
     * @param handlerId 处理人ID
     * @param operator 操作人
     * @return 退款记录
     */
    public DigitalRefundRecord cancelDigitalRefund(Long refundId, DigitalRefundRecord refund,
            Long handlerId, String operator);
}
