package com.dop.digital.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.dop.digital.domain.DigitalAfterSaleLog;

/**
 * 售后处理记录 数据层
 *
 * @author dop
 */
public interface DigitalAfterSaleLogMapper
{
    /**
     * 查询工单处理记录
     *
     * @param ticketId 工单ID
     * @return 处理记录集合
     */
    public List<DigitalAfterSaleLog> selectDigitalAfterSaleLogList(@Param("ticketId") Long ticketId);

    /**
     * 新增处理记录
     *
     * @param log 处理记录
     * @return 结果
     */
    public int insertDigitalAfterSaleLog(DigitalAfterSaleLog log);
}
