package com.dede.tester.ui.svn

import java.io.File
import java.io.FileOutputStream

/**
 * 带进度的文件输出流
 * @author hsh
 * @since 2020/8/21 2:17 PM
 */
class ProgressFileOutputStream(private val totalSize: Long, file: File) : FileOutputStream(file) {

    private var current = 0L

    var isClosed = false
        private set

    var percent = 0f
        private set

    override fun write(b: ByteArray, off: Int, len: Int) {
        super.write(b, off, len)
        current += len
        percent = current * 1f / totalSize
    }

    override fun close() {
        super.close()
        isClosed = true
    }
}