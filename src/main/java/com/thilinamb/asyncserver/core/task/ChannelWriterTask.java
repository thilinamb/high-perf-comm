package com.thilinamb.asyncserver.core.task;

import java.nio.channels.SelectionKey;

/**
 * @author Thilina Buddhika
 */
public class ChannelWriterTask extends Task {

    public ChannelWriterTask(SelectionKey selectionKey) {
        super(selectionKey);
    }

    @Override
    public void run() {

    }
}
