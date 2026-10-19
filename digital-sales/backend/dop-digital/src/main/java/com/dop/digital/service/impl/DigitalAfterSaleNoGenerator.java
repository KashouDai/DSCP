package com.dop.digital.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.dop.common.exception.ServiceException;
import com.dop.common.utils.StringUtils;
import com.dop.digital.mapper.DigitalNoSeqMapper;

/**
 * 售后工单编号生成器
 *
 * @author dop
 */
@Service
public class DigitalAfterSaleNoGenerator
{
    private static final String AFTER_SALE_SEQ_TYPE = "AS";

    private static final int MAX_SEQUENCE = 9999;

    @Autowired
    private DigitalNoSeqMapper noSeqMapper;

    /**
     * 使用独立事务生成售后工单编号。
     *
     * @return 工单编号
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public String nextTicketNo()
    {
        noSeqMapper.increaseSequence(AFTER_SALE_SEQ_TYPE);
        Integer sequence = noSeqMapper.selectCurrentSequence(AFTER_SALE_SEQ_TYPE);
        String currentDate = noSeqMapper.selectCurrentDate();
        if (StringUtils.isNull(sequence) || StringUtils.isEmpty(currentDate))
        {
            throw new ServiceException("生成售后工单编号失败");
        }
        if (sequence > MAX_SEQUENCE)
        {
            throw new ServiceException("当日售后工单数量已超过编号上限");
        }
        return "AS" + currentDate + String.format("%04d", sequence);
    }
}
