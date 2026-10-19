package com.dop.digital.service;

import java.util.List;
import com.dop.digital.domain.DigitalCustomer;

/**
 * 客户 服务层
 *
 * @author dop
 */
public interface IDigitalCustomerService
{
    /**
     * 查询客户列表
     *
     * @param customer 客户
     * @return 客户集合
     */
    public List<DigitalCustomer> selectDigitalCustomerList(DigitalCustomer customer);

    /**
     * 根据客户ID查询客户
     *
     * @param customerId 客户ID
     * @return 客户
     */
    public DigitalCustomer selectDigitalCustomerById(Long customerId);

    /**
     * 新增客户
     *
     * @param customer 客户
     * @return 结果
     */
    public int insertDigitalCustomer(DigitalCustomer customer);

    /**
     * 修改客户
     *
     * @param customer 客户
     * @return 结果
     */
    public int updateDigitalCustomer(DigitalCustomer customer);

    /**
     * 批量删除客户
     *
     * @param customerIds 客户ID数组
     * @return 结果
     */
    public int deleteDigitalCustomerByIds(Long[] customerIds);
}
