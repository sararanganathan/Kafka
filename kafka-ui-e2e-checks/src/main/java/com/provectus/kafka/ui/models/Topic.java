package com.provectus.kafka.ui.models;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class Topic {
    private String name, message, compactPolicyValue, timeToRetainData, maxSizeOnDisk, maxMessageBytes, messageKey, messageContent ;
}
