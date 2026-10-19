package com.dop.digital.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.dop.common.exception.ServiceException;
import com.dop.common.utils.StringUtils;
import com.dop.digital.domain.DigitalCustomer;
import com.dop.digital.mapper.DigitalCustomerMapper;
import com.dop.digital.mapper.DigitalSaleOrderMapper;
import com.dop.digital.service.IDigitalCustomerService;

/**
 * 客户 服务层实现
 *
 * @author dop
 */
@Service
public class DigitalCustomerServiceImpl implements IDigitalCustomerService
{
    @Autowired
    private DigitalCustomerMapper customerMapper;

    @Autowired
    private DigitalSaleOrderMapper orderMapper;

    @Override
    public List<DigitalCustomer> selectDigitalCustomerList(DigitalCustomer customer)
    {
        return customerMapper.selectDigitalCustomerList(customer);
    }

    @Override
    public DigitalCustomer selectDigitalCustomerById(Long customerId)
    {
        return customerMapper.selectDigitalCustomerById(customerId);
    }

    @Override
    public int insertDigitalCustomer(DigitalCustomer customer)
    {
        return customerMapper.insertDigitalCustomer(customer);
    }

    @Override
    public int updateDigitalCustomer(DigitalCustomer customer)
    {
        return customerMapper.updateDigitalCustomer(customer);
    }

    @Override
    public int deleteDigitalCustomerByIds(Long[] customerIds)
    {
        for (Long customerId : customerIds)
        {
            DigitalCustomer customer = customerMapper.selectDigitalCustomerById(customerId);
            if (StringUtils.isNotNull(customer) && orderMapper.countOrderByCustomerId(customerId) > 0)
            {
                throw new ServiceException(String.format("%1$s已被订单引用，只能停用，不能删除", customer.getCustomerName()));
            }
        }
        return customerMapper.deleteDigitalCustomerByIds(customerIds);
    }
}
