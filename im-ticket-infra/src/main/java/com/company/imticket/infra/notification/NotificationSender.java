package com.company.imticket.infra.notification;

import com.company.imticket.dao.entity.NotificationTemplate;
import com.company.imticket.dao.mapper.NotificationTemplateMapper;
import com.company.imticket.infra.channel.ChannelAdapter;
import com.company.imticket.infra.mq.MqConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class NotificationSender {

    private static final Logger log = LoggerFactory.getLogger(NotificationSender.class);
    private final NotificationTemplateMapper templateMapper;
    private final List<ChannelAdapter> channelAdapters;
    private final RabbitTemplate rabbitTemplate;
    private static final Pattern VAR_PATTERN = Pattern.compile("\\{\\{(\\w+)\\}\\}");

    public NotificationSender(NotificationTemplateMapper templateMapper,
                              List<ChannelAdapter> channelAdapters,
                              RabbitTemplate rabbitTemplate) {
        this.templateMapper = templateMapper;
        this.channelAdapters = channelAdapters;
        this.rabbitTemplate = rabbitTemplate;
    }

    public void sendByTemplate(String templateCode, Map<String, String> variables,
                                String targetUserId, String targetGroupId) {
        NotificationTemplate template = templateMapper.findByCode(templateCode);
        if (template == null) {
            log.error("通知模板不存在: {}", templateCode);
            return;
        }

        String content = renderTemplate(template.getContent(), variables);
        sendAsync(template, content, targetUserId, targetGroupId);
    }

    private String renderTemplate(String template, Map<String, String> variables) {
        Matcher matcher = VAR_PATTERN.matcher(template);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String varName = matcher.group(1);
            String replacement = variables.getOrDefault(varName, "");
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private void sendAsync(NotificationTemplate template, String content,
                           String targetUserId, String targetGroupId) {
        NotificationMessage msg = new NotificationMessage();
        msg.setTemplateCode(template.getCode());
        msg.setChannel(template.getChannel());
        msg.setContent(content);
        msg.setTargetUserId(targetUserId);
        msg.setTargetGroupId(targetGroupId);
        rabbitTemplate.convertAndSend(MqConstants.EXCHANGE_IM,
                MqConstants.ROUTING_KEY_NOTIFICATION, msg);
    }
}