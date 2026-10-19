package com.dop.digital.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.dop.common.core.domain.BaseEntity;

/**
 * 售后处理记录对象 biz_after_sale_log
 *
 * @author dop
 */
public class DigitalAfterSaleLog extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 记录ID */
    private Long logId;

    /** 工单ID */
    private Long ticketId;

    /** 操作类型 */
    private String operateType;

    /** 操作内容 */
    private String operateContent;

    /** 附件 */
    private String attachmentUrls;

    public Long getLogId()
    {
        return logId;
    }

    public void setLogId(Long logId)
    {
        this.logId = logId;
    }

    public Long getTicketId()
    {
        return ticketId;
    }

    public void setTicketId(Long ticketId)
    {
        this.ticketId = ticketId;
    }

    @NotBlank(message = "操作类型不能为空")
    public String getOperateType()
    {
        return operateType;
    }

    public void setOperateType(String operateType)
    {
        this.operateType = operateType;
    }

    @Size(max = 1000, message = "操作内容长度不能超过1000个字符")
    public String getOperateContent()
    {
        return operateContent;
    }

    public void setOperateContent(String operateContent)
    {
        this.operateContent = operateContent;
    }

    @Size(max = 2000, message = "附件长度不能超过2000个字符")
    public String getAttachmentUrls()
    {
        return attachmentUrls;
    }

    public void setAttachmentUrls(String attachmentUrls)
    {
        this.attachmentUrls = attachmentUrls;
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("logId", getLogId())
            .append("ticketId", getTicketId())
            .append("operateType", getOperateType())
            .append("operateContent", getOperateContent())
            .append("attachmentUrls", getAttachmentUrls())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .toString();
    }
}
