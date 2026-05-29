package com.company.imticket.infra.mq;

public class MqConstants {
    public static final String EXCHANGE_IM = "im.topic";
    public static final String QUEUE_MESSAGE_IN = "im.message.in";
    public static final String QUEUE_NOTIFICATION = "im.notification";
    public static final String QUEUE_MESSAGE_DLX = "im.message.dlx";
    public static final String ROUTING_KEY_MESSAGE = "im.message.*";
    public static final String ROUTING_KEY_NOTIFICATION = "im.notification.*";

    private MqConstants() {}
}