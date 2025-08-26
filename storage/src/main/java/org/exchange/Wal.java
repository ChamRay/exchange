package org.exchange;

public interface Wal extends AutoCloseable {
    void append(byte[] record);              // 追加(内部批量落盘)

    void flush();                            // 定时/阈值触发 fsync

    WalCursor replay(long fromOffset);       // 用于重放
}
