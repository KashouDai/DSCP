package com.dop.digital.mapper;

import java.util.List;
import com.dop.digital.domain.DigitalStockRecord;

/**
 * 库存流水 数据层
 *
 * @author dop
 */
public interface DigitalStockRecordMapper
{
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
     * 新增库存流水
     *
     * @param record 库存流水
     * @return 结果
     */
    public int insertDigitalStockRecord(DigitalStockRecord record);
}
