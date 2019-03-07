package com.brufino.android.playground.transfer;

import androidx.annotation.NonNull;
import com.brufino.android.playground.extensions.StringUtils;
import com.brufino.android.playground.extensions.ViewUtils;

import java.io.Serializable;
import java.util.Locale;

import static com.brufino.android.playground.extensions.StringUtils.indent;
import static com.brufino.android.playground.extensions.ViewUtils.sizeString;

public class TransferConfiguration implements Serializable {
    public static final TransferConfiguration DEFAULT =
            new TransferConfiguration(
                    /* producerDataSize */ 128 * 1024,
                    /* producerInterval */ 50,
                    /* producerChunkSize */ 64 * 1024,
                    /* transferBufferSize */ 8 * 1024,
                    /* consumerInterval */ 50,
                    /* consumerBufferSize */ 32 * 1024);

    private static final long serialVersionUID = -6376452245722381096L;

    public final int producerDataSize;
    public final int producerInterval;
    public final int producerChunkSize;
    public final int transferBufferSize;
    public final int consumerInterval;
    public final int consumerBufferSize;

    public TransferConfiguration(
            int producerDataSize,
            int producerInterval,
            int producerChunkSize,
            int transferBufferSize,
            int consumerInterval,
            int consumerBufferSize) {
        this.producerDataSize = producerDataSize;
        this.producerInterval = producerInterval;
        this.producerChunkSize = producerChunkSize;
        this.transferBufferSize = transferBufferSize;
        this.consumerInterval = consumerInterval;
        this.consumerBufferSize = consumerBufferSize;
    }

    @Override
    public String toString() {
        return "TransferConfiguration{"
                + "producer data = " + sizeString(producerDataSize) + ", "
                + "producer interval = " + producerInterval + " ms, "
                + "producer chunk = " + sizeString(producerChunkSize) + ", "
                + "transfer buffer = " + sizeString(transferBufferSize) + ", "
                + "consumer interval = " + consumerInterval + " ms, "
                + "consumer buffer = " + sizeString(consumerBufferSize) + "}";
    }

    public String toMultilineString(int i) {
        return "TransferConfiguration{\n"
                + indent(i) + "producer data = " + sizeString(producerDataSize) + "\n"
                + indent(i) + "producer interval = " + producerInterval + " ms\n"
                + indent(i) + "producer chunk = " + sizeString(producerChunkSize) + "\n"
                + indent(i) + "transfer buffer = " + sizeString(transferBufferSize) + "\n"
                + indent(i) + "consumer interval = " + consumerInterval + " ms\n"
                + indent(i) + "consumer buffer = " + sizeString(consumerBufferSize) + "\n"
                + indent(i - 1) + "}";
    }
}
