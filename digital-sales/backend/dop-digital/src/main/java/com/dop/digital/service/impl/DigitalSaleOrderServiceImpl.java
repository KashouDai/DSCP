package com.dop.digital.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.dop.common.exception.ServiceException;
import com.dop.common.utils.StringUtils;
import com.dop.digital.domain.DigitalCustomer;
import com.dop.digital.domain.DigitalProductSku;
import com.dop.digital.domain.DigitalSaleOrder;
import com.dop.digital.domain.DigitalSaleOrderItem;
import com.dop.digital.domain.DigitalStockRecord;
import com.dop.digital.mapper.DigitalCustomerMapper;
import com.dop.digital.mapper.DigitalProductSkuMapper;
import com.dop.digital.mapper.DigitalSaleOrderItemMapper;
import com.dop.digital.mapper.DigitalSaleOrderMapper;
import com.dop.digital.mapper.DigitalStockRecordMapper;
import com.dop.digital.service.IDigitalSaleOrderService;

/**
 * 销售订单 服务层实现
 *
 * @author dop
 */
@Service
public class DigitalSaleOrderServiceImpl implements IDigitalSaleOrderService
{
    private static final String STATUS_DRAFT = "DRAFT";

    private static final String STATUS_CONFIRMED = "CONFIRMED";

    private static final String STATUS_COMPLETED = "COMPLETED";

    private static final String STATUS_CANCELLED = "CANCELLED";

    private static final String CUSTOMER_STATUS_NORMAL = "0";

    private static final String RECORD_TYPE_SALE_OUT = "SALE_OUT";

    private static final String RECORD_TYPE_CANCEL_IN = "CANCEL_IN";

    private static final int MAX_ORDER_NO_RETRY = 3;

    @Autowired
    private DigitalSaleOrderMapper orderMapper;

    @Autowired
    private DigitalSaleOrderItemMapper orderItemMapper;

    @Autowired
    private DigitalCustomerMapper customerMapper;

    @Autowired
    private DigitalProductSkuMapper skuMapper;

    @Autowired
    private DigitalStockRecordMapper stockRecordMapper;

    @Autowired
    private DigitalOrderNoGenerator orderNoGenerator;

    @Override
    public List<DigitalSaleOrder> selectDigitalSaleOrderList(DigitalSaleOrder order)
    {
        return orderMapper.selectDigitalSaleOrderList(order);
    }

    @Override
    public DigitalSaleOrder selectDigitalSaleOrderById(Long orderId)
    {
        return loadOrder(orderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DigitalSaleOrder createDigitalSaleOrderDraft(DigitalSaleOrder order, String operator)
    {
        String requestNo = normalizeRequestNo(order);
        prepareDraft(order, operator);
        order.setRequestNo(requestNo);
        order.setRequestHash(calculateRequestHash(order));

        DigitalSaleOrder existing = orderMapper.selectDigitalSaleOrderByRequestNo(requestNo);
        if (StringUtils.isNotNull(existing))
        {
            return resolveIdempotentOrder(existing, order.getRequestHash());
        }

        order.setStatus(STATUS_DRAFT);
        order.setCreateBy(operator);
        for (int attempt = 0; attempt < MAX_ORDER_NO_RETRY; attempt++)
        {
            order.setOrderId(null);
            order.setOrderNo(orderNoGenerator.nextOrderNo());
            try
            {
                if (orderMapper.insertDigitalSaleOrder(order) != 1)
                {
                    throw new ServiceException("创建订单失败");
                }
                insertOrderItems(order, operator);
                return loadOrder(order.getOrderId());
            }
            catch (DuplicateKeyException ex)
            {
                existing = orderMapper.selectDigitalSaleOrderByRequestNo(requestNo);
                if (StringUtils.isNotNull(existing))
                {
                    return resolveIdempotentOrder(existing, order.getRequestHash());
                }
                if (attempt == MAX_ORDER_NO_RETRY - 1)
                {
                    throw new ServiceException("生成订单编号失败，请重试");
                }
            }
        }
        throw new ServiceException("创建订单失败");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DigitalSaleOrder updateDigitalSaleOrderDraft(DigitalSaleOrder order, String operator)
    {
        if (StringUtils.isNull(order) || StringUtils.isNull(order.getOrderId()))
        {
            throw new ServiceException("订单不能为空");
        }
        DigitalSaleOrder current = loadOrderForUpdate(order.getOrderId());
        if (StringUtils.isNull(current))
        {
            throw new ServiceException("订单不存在");
        }
        if (!STATUS_DRAFT.equals(current.getStatus()))
        {
            throw new ServiceException("只有草稿订单可以修改");
        }

        prepareDraft(order, operator);
        order.setUpdateBy(operator);
        if (orderMapper.updateDigitalSaleOrderDraft(order) != 1)
        {
            throw new ServiceException("订单状态已变化，请刷新后重试");
        }
        orderItemMapper.deleteDigitalSaleOrderItemByOrderId(order.getOrderId());
        insertOrderItems(order, operator);
        return loadOrder(order.getOrderId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DigitalSaleOrder confirmDigitalSaleOrder(Long orderId, String operator)
    {
        DigitalSaleOrder order = loadOrderForUpdate(orderId);
        requireOrder(order);
        if (STATUS_CONFIRMED.equals(order.getStatus()))
        {
            return order;
        }
        if (!STATUS_DRAFT.equals(order.getStatus()))
        {
            throw new ServiceException("只有草稿订单可以确认");
        }

        int rows = orderMapper.confirmDigitalSaleOrder(orderId, operator);
        if (rows != 1)
        {
            DigitalSaleOrder current = loadOrderForUpdate(orderId);
            if (StringUtils.isNotNull(current) && STATUS_CONFIRMED.equals(current.getStatus()))
            {
                return current;
            }
            throw new ServiceException("订单状态已变化，请刷新后重试");
        }

        List<DigitalSaleOrderItem> items = new ArrayList<>(
                orderItemMapper.selectDigitalSaleOrderItemList(orderId));
        if (items.isEmpty())
        {
            throw new ServiceException("订单明细不能为空");
        }
        items.sort(Comparator.comparing(DigitalSaleOrderItem::getSkuId));
        for (DigitalSaleOrderItem item : items)
        {
            DigitalProductSku sku = skuMapper.selectDigitalProductSkuById(item.getSkuId());
            if (StringUtils.isNull(sku))
            {
                throw new ServiceException("订单商品规格不存在或已删除");
            }
            if (skuMapper.decreaseStock(item.getSkuId(), item.getQuantity(), operator) != 1)
            {
                throw new ServiceException("商品规格[" + sku.getProductName() + " / " + sku.getSkuName()
                        + "]库存不足");
            }
            DigitalProductSku updatedSku = skuMapper.selectDigitalProductSkuById(item.getSkuId());
            if (StringUtils.isNull(updatedSku) || StringUtils.isNull(updatedSku.getStock()))
            {
                throw new ServiceException("读取库存失败");
            }
            insertStockRecord(item.getSkuId(), RECORD_TYPE_SALE_OUT, -1, item.getQuantity(),
                    updatedSku.getStock() + item.getQuantity(), updatedSku.getStock(), orderId, operator,
                    "订单确认：" + order.getOrderNo());
        }
        return loadOrder(orderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DigitalSaleOrder cancelDigitalSaleOrder(Long orderId, String operator)
    {
        DigitalSaleOrder order = loadOrderForUpdate(orderId);
        requireOrder(order);
        if (STATUS_CANCELLED.equals(order.getStatus()))
        {
            return order;
        }
        if (STATUS_COMPLETED.equals(order.getStatus()))
        {
            throw new ServiceException("已完成订单不能取消");
        }

        if (STATUS_DRAFT.equals(order.getStatus()))
        {
            if (orderMapper.cancelDigitalSaleOrderDraft(orderId, operator) != 1)
            {
                throw new ServiceException("订单状态已变化，请刷新后重试");
            }
            return loadOrder(orderId);
        }
        if (!STATUS_CONFIRMED.equals(order.getStatus()))
        {
            throw new ServiceException("当前订单状态不能取消");
        }

        if (orderMapper.cancelDigitalSaleOrderConfirmed(orderId, operator) != 1)
        {
            DigitalSaleOrder current = loadOrderForUpdate(orderId);
            if (StringUtils.isNotNull(current) && STATUS_CANCELLED.equals(current.getStatus()))
            {
                return current;
            }
            throw new ServiceException("订单状态已变化，请刷新后重试");
        }

        List<DigitalSaleOrderItem> items = new ArrayList<>(
                orderItemMapper.selectDigitalSaleOrderItemList(orderId));
        items.sort(Comparator.comparing(DigitalSaleOrderItem::getSkuId));
        for (DigitalSaleOrderItem item : items)
        {
            if (skuMapper.increaseStock(item.getSkuId(), item.getQuantity(), operator) != 1)
            {
                throw new ServiceException("恢复商品库存失败");
            }
            DigitalProductSku updatedSku = skuMapper.selectDigitalProductSkuById(item.getSkuId());
            if (StringUtils.isNull(updatedSku) || StringUtils.isNull(updatedSku.getStock()))
            {
                throw new ServiceException("读取库存失败");
            }
            insertStockRecord(item.getSkuId(), RECORD_TYPE_CANCEL_IN, 1, item.getQuantity(),
                    updatedSku.getStock() - item.getQuantity(), updatedSku.getStock(), orderId, operator,
                    "订单取消：" + order.getOrderNo());
        }
        return loadOrder(orderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DigitalSaleOrder completeDigitalSaleOrder(Long orderId, String operator)
    {
        DigitalSaleOrder order = loadOrderForUpdate(orderId);
        requireOrder(order);
        if (STATUS_COMPLETED.equals(order.getStatus()))
        {
            return order;
        }
        if (!STATUS_CONFIRMED.equals(order.getStatus()))
        {
            throw new ServiceException("只有已确认订单可以完成");
        }
        if (orderMapper.completeDigitalSaleOrder(orderId, operator) != 1)
        {
            DigitalSaleOrder current = loadOrderForUpdate(orderId);
            if (StringUtils.isNotNull(current) && STATUS_COMPLETED.equals(current.getStatus()))
            {
                return current;
            }
            throw new ServiceException("订单状态已变化，请刷新后重试");
        }
        return loadOrder(orderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteDigitalSaleOrderByIds(Long[] orderIds)
    {
        if (StringUtils.isEmpty(orderIds))
        {
            throw new ServiceException("订单不能为空");
        }
        List<DigitalSaleOrder> orders = orderMapper.selectDigitalSaleOrderByIds(orderIds);
        if (orders.isEmpty())
        {
            return 0;
        }
        for (DigitalSaleOrder order : orders)
        {
            if (!STATUS_DRAFT.equals(order.getStatus()))
            {
                throw new ServiceException("只有草稿订单可以删除");
            }
        }
        orderItemMapper.deleteDigitalSaleOrderItemByOrderIds(orderIds);
        int rows = orderMapper.deleteDigitalSaleOrderByIds(orderIds);
        if (rows != orders.size())
        {
            throw new ServiceException("订单状态已变化，请刷新后重试");
        }
        return rows;
    }

    private void prepareDraft(DigitalSaleOrder order, String operator)
    {
        if (StringUtils.isNull(order))
        {
            throw new ServiceException("订单不能为空");
        }
        if (StringUtils.isNull(order.getCustomerId()))
        {
            throw new ServiceException("客户不能为空");
        }
        DigitalCustomer customer = customerMapper.selectDigitalCustomerById(order.getCustomerId());
        if (StringUtils.isNull(customer) || !CUSTOMER_STATUS_NORMAL.equals(customer.getStatus()))
        {
            throw new ServiceException("客户不存在或已停用");
        }
        List<DigitalSaleOrderItem> items = normalizeItems(order.getItems(), operator);
        if (items.isEmpty())
        {
            throw new ServiceException("订单明细不能为空");
        }

        BigDecimal totalAmount = BigDecimal.ZERO;
        for (DigitalSaleOrderItem item : items)
        {
            totalAmount = totalAmount.add(item.getSalePrice()
                    .multiply(BigDecimal.valueOf(item.getQuantity())));
        }
        order.setCustomerName(customer.getCustomerName());
        order.setCustomerPhone(customer.getPhone());
        order.setTotalAmount(totalAmount.setScale(2, RoundingMode.HALF_UP));
        order.setItems(items);
    }

    private String normalizeRequestNo(DigitalSaleOrder order)
    {
        if (StringUtils.isNull(order) || StringUtils.isEmpty(order.getRequestNo()))
        {
            throw new ServiceException("订单创建请求号不能为空");
        }
        String requestNo = order.getRequestNo().trim();
        if (requestNo.isEmpty())
        {
            throw new ServiceException("订单创建请求号不能为空");
        }
        if (requestNo.length() > 64)
        {
            throw new ServiceException("订单创建请求号长度不能超过64个字符");
        }
        return requestNo;
    }

    private String calculateRequestHash(DigitalSaleOrder order)
    {
        StringBuilder content = new StringBuilder();
        content.append(order.getCustomerId()).append('|');
        content.append(order.getRemark() == null ? "" : order.getRemark()).append('|');

        List<DigitalSaleOrderItem> items = new ArrayList<>(order.getItems());
        items.sort(Comparator.comparing(DigitalSaleOrderItem::getSkuId));
        for (DigitalSaleOrderItem item : items)
        {
            content.append(item.getSkuId()).append(':')
                .append(item.getQuantity()).append(':')
                .append(item.getSalePrice().setScale(2, RoundingMode.HALF_UP).toPlainString())
                .append(';');
        }

        try
        {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(content.toString().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        }
        catch (NoSuchAlgorithmException ex)
        {
            throw new ServiceException("生成订单请求摘要失败");
        }
    }

    private DigitalSaleOrder resolveIdempotentOrder(DigitalSaleOrder existing, String requestHash)
    {
        if (!requestHash.equals(existing.getRequestHash()))
        {
            throw new ServiceException("订单创建请求号已用于其他订单");
        }
        return loadOrder(existing.getOrderId());
    }

    private List<DigitalSaleOrderItem> normalizeItems(List<DigitalSaleOrderItem> items, String operator)
    {
        if (items == null || items.isEmpty())
        {
            return new ArrayList<>();
        }
        Map<Long, DigitalSaleOrderItem> itemMap = new LinkedHashMap<>();
        for (DigitalSaleOrderItem item : items)
        {
            if (StringUtils.isNull(item) || StringUtils.isNull(item.getSkuId()))
            {
                throw new ServiceException("订单商品规格不能为空");
            }
            if (StringUtils.isNull(item.getQuantity()) || item.getQuantity() <= 0)
            {
                throw new ServiceException("购买数量必须大于0");
            }
            if (StringUtils.isNull(item.getSalePrice()) || item.getSalePrice().compareTo(BigDecimal.ZERO) < 0)
            {
                throw new ServiceException("成交单价不能小于0");
            }

            DigitalProductSku sku = skuMapper.selectDigitalProductSkuById(item.getSkuId());
            if (StringUtils.isNull(sku))
            {
                throw new ServiceException("订单商品规格不存在或已删除");
            }

            DigitalSaleOrderItem existing = itemMap.get(item.getSkuId());
            if (StringUtils.isNotNull(existing))
            {
                if (existing.getSalePrice().compareTo(item.getSalePrice()) != 0)
                {
                    throw new ServiceException("同一商品规格的成交单价必须一致");
                }
                existing.setQuantity(existing.getQuantity() + item.getQuantity());
                continue;
            }

            DigitalSaleOrderItem normalized = new DigitalSaleOrderItem();
            normalized.setSkuId(sku.getSkuId());
            normalized.setProductName(sku.getProductName());
            normalized.setSkuName(sku.getSkuName());
            normalized.setSalePrice(item.getSalePrice());
            normalized.setQuantity(item.getQuantity());
            normalized.setRemark(item.getRemark());
            normalized.setCreateBy(operator);
            itemMap.put(sku.getSkuId(), normalized);
        }
        return new ArrayList<>(itemMap.values());
    }

    private void insertOrderItems(DigitalSaleOrder order, String operator)
    {
        for (DigitalSaleOrderItem item : order.getItems())
        {
            item.setOrderId(order.getOrderId());
            item.setCreateBy(operator);
        }
        int rows = orderItemMapper.batchInsertDigitalSaleOrderItem(order.getItems());
        if (rows != order.getItems().size())
        {
            throw new ServiceException("保存订单明细失败");
        }
    }

    private void insertStockRecord(Long skuId, String recordType, Integer direction, Integer quantity,
            Integer beforeStock, Integer afterStock, Long orderId, String operator, String remark)
    {
        DigitalStockRecord record = new DigitalStockRecord();
        record.setSkuId(skuId);
        record.setRecordType(recordType);
        record.setChangeDirection(direction);
        record.setQuantity(quantity);
        record.setBeforeStock(beforeStock);
        record.setAfterStock(afterStock);
        record.setBizType(recordType);
        record.setBizId(orderId);
        record.setCreateBy(operator);
        record.setRemark(remark);
        stockRecordMapper.insertDigitalStockRecord(record);
    }

    private DigitalSaleOrder loadOrder(Long orderId)
    {
        if (StringUtils.isNull(orderId))
        {
            return null;
        }
        DigitalSaleOrder order = orderMapper.selectDigitalSaleOrderById(orderId);
        if (StringUtils.isNotNull(order))
        {
            order.setItems(orderItemMapper.selectDigitalSaleOrderItemList(orderId));
        }
        return order;
    }

    private DigitalSaleOrder loadOrderForUpdate(Long orderId)
    {
        if (StringUtils.isNull(orderId))
        {
            return null;
        }
        DigitalSaleOrder order = orderMapper.selectDigitalSaleOrderByIdForUpdate(orderId);
        if (StringUtils.isNotNull(order))
        {
            order.setItems(orderItemMapper.selectDigitalSaleOrderItemList(orderId));
        }
        return order;
    }

    private void requireOrder(DigitalSaleOrder order)
    {
        if (StringUtils.isNull(order))
        {
            throw new ServiceException("订单不存在");
        }
    }
}
