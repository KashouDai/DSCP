package com.dop.digital.service;

import java.util.List;
import com.dop.digital.domain.DigitalProductSku;

/**
 * 商品规格 服务层
 *
 * @author dop
 */
public interface IDigitalProductSkuService
{
    /**
     * 查询商品规格列表
     *
     * @param sku 商品规格
     * @return 商品规格集合
     */
    public List<DigitalProductSku> selectDigitalProductSkuList(DigitalProductSku sku);

    /**
     * 根据规格ID查询商品规格
     *
     * @param skuId 规格ID
     * @return 商品规格
     */
    public DigitalProductSku selectDigitalProductSkuById(Long skuId);

    /**
     * 校验规格编码是否唯一
     *
     * @param sku 商品规格
     * @return 结果
     */
    public boolean checkSkuCodeUnique(DigitalProductSku sku);

    public boolean checkBarcodeUnique(DigitalProductSku sku);

    public String generateBarcode();

    /**
     * 新增商品规格
     *
     * @param sku 商品规格
     * @return 结果
     */
    public int insertDigitalProductSku(DigitalProductSku sku);

    /**
     * 修改商品规格
     *
     * @param sku 商品规格
     * @return 结果
     */
    public int updateDigitalProductSku(DigitalProductSku sku);

    /**
     * 逻辑删除商品规格
     *
     * @param skuIds 规格ID数组
     * @return 结果
     */
    public int deleteDigitalProductSkuByIds(Long[] skuIds);
}
