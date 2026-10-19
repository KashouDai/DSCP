package com.dop.digital.mapper;

import java.math.BigDecimal;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.dop.digital.domain.DigitalRefundRecord;

/**
 * 退款记录 数据层
 *
 * @author dop
 */
public interface DigitalRefundRecordMapper
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
     * 根据退款ID锁定退款记录
     *
     * @param refundId 退款ID
     * @return 退款记录
     */
    public DigitalRefundRecord selectDigitalRefundRecordByIdForUpdate(Long refundId);

    /**
     * 新增退款记录
     *
     * @param refund 退款记录
     * @return 结果
     */
    public int insertDigitalRefundRecord(DigitalRefundRecord refund);

    /**
     * 完成退款审核
     *
     * @param refundId 退款ID
     * @param refundAmount 退款金额
     * @param handlerId 处理人ID
     * @param operator 操作人
     * @return 结果
     */
    public int auditDigitalRefund(@Param("refundId") Long refundId,
            @Param("refundAmount") BigDecimal refundAmount, @Param("handlerId") Long handlerId,
            @Param("operator") String operator);

    /**
     * 取消退款
     *
     * @param refundId 退款ID
     * @param remark 取消原因
     * @param handlerId 处理人ID
     * @param operator 操作人
     * @return 结果
     */
    public int cancelDigitalRefund(@Param("refundId") Long refundId, @Param("remark") String remark,
            @Param("handlerId") Long handlerId, @Param("operator") String operator);
}
