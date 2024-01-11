package com.example.bluetoothconnection.communication.Utils.PayloadDataEntities;

import static com.example.bluetoothconnection.communication.Utils.Common.MessageContentType;

public class PayloadData {
    private final MessageContentType messageContentType;

    public PayloadData(MessageContentType messageContentType) {
        this.messageContentType = messageContentType;
    }

    public MessageContentType getMessageContentType() {
        return messageContentType;
    }
}
