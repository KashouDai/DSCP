package com.dop.digital.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.dop.digital.domain.DigitalProductSku;

/**
 * 商品规格 数据层
 *
 * @author dop
 */
public interface DigitalProductSkuMapper
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
     * @param skuCode 规格编码
     * @return 商品规格
     */
    public DigitalProductSku checkSkuCodeUnique(String skuCode);

    public DigitalProductSku checkBarcodeUnique(@Param("barcode") String barcode, @Param("skuId") Long skuId);

    /**
     * 统计商品下的规格数量
     *
     * @param productId 商品ID
     * @return 规格数量
     */
    public int countSkuByProductId(Long productId);

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

    /**
     * 增加库存
     *
     * @param skuId 规格ID
     * @param quantity 入库数量
     * @param updateBy 更新者
     * @return 结果
     */
    public int increaseStock(@Param("skuId") Long skuId, @Param("quantity") Integer quantity,
            @Param("updateBy") String updateBy);

    /**
     * 扣减库存
     *
     * @param skuId 规格ID
     * @param quantity 扣减数量
     * @param updateBy 更新者
     * @return 结果
     */
    public int decreaseStock(@Param("skuId") Long skuId, @Param("quantity") Integer quantity,
            @Param("updateBy") String updateBy);
}
