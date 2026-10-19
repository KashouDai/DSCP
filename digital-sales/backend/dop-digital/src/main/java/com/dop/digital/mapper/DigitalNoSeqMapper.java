package com.dop.digital.mapper;

import org.apache.ibatis.annotations.Param;

/**
 * 业务编号流水 数据层
 *
 * @author dop
 */
public interface DigitalNoSeqMapper
{
    /**
     * 递增并锁定当日业务编号
     *
     * @param seqType 编号类型
     * @return 结果
     */
    public int increaseSequence(@Param("seqType") String seqType);

    /**
     * 查询当日业务编号
     *
     * @param seqType 编号类型
     * @return 当前流水值
     */
    public Integer selectCurrentSequence(@Param("seqType") String seqType);

    /**
     * 查询数据库当前日期
     *
     * @return 当前日期，格式为yyyyMMdd
     */
    public String selectCurrentDate();
}
