package com.dop.digital.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.dop.common.exception.ServiceException;
import com.dop.common.utils.StringUtils;
import com.dop.digital.mapper.DigitalNoSeqMapper;

/**
 * 销售订单编号生成器
 *
 * @author dop
 */
@Service
public class DigitalOrderNoGenerator
{
    private static final String ORDER_SEQ_TYPE = "SO";

    private static final int MAX_SEQUENCE = 9999;

    @Autowired
    private DigitalNoSeqMapper noSeqMapper;

    /**
     * 使用独立事务生成订单编号，避免订单回滚时重复占用编号。
     *
     * @return 订单编号
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public String nextOrderNo()
    {
        noSeqMapper.increaseSequence(ORDER_SEQ_TYPE);
        Integer sequence = noSeqMapper.selectCurrentSequence(ORDER_SEQ_TYPE);
        String currentDate = noSeqMapper.selectCurrentDate();
        if (StringUtils.isNull(sequence) || StringUtils.isEmpty(currentDate))
        {
            throw new ServiceException("生成订单编号失败");
        }
        if (sequence > MAX_SEQUENCE)
        {
            throw new ServiceException("当日订单数量已超过编号上限");
        }
        return "SO" + currentDate + String.format("%04d", sequence);
    }
}
