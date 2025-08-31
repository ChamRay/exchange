package org.exchange;

import java.nio.channels.FileChannel;
import java.nio.ByteBuffer;
import java.nio.file.*;

public final class FileChannelWal implements Wal {
    private final FileChannel ch;
    private final ByteBuffer len = ByteBuffer.allocateDirect(4);

    public FileChannelWal(Path p) throws Exception {
        ch = FileChannel.open(p, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.READ);
        ch.position(ch.size());
    }

    public void append(byte[] rec) {
        try {
            len.clear();
            len.putInt(rec.length).flip();
            ch.write(len);
            ch.write(ByteBuffer.wrap(rec));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void flush() {
        try {
            ch.force(false);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public WalCursor replay(long from) {
        return new WalCursor(() -> ch, from);
    }

    public void close() throws Exception {
        ch.close();
    }
}
