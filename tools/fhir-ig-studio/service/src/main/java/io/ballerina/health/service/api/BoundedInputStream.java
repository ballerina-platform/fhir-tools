/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.ballerina.health.service.api;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Fails fast once a request body exceeds the configured upload limit, instead of buffering it to disk first.
 */
public class BoundedInputStream extends FilterInputStream {

    private final long limit;
    private long consumed;

    public BoundedInputStream(InputStream delegate, long limit) {
        super(delegate);
        this.limit = limit;
    }

    @Override
    public int read() throws IOException {
        int value = super.read();
        if (value >= 0) {
            track(1);
        }
        return value;
    }

    @Override
    public int read(byte[] buffer, int offset, int length) throws IOException {
        int read = super.read(buffer, offset, length);
        if (read > 0) {
            track(read);
        }
        return read;
    }

    private void track(int count) throws IOException {
        consumed += count;
        if (consumed > limit) {
            throw new LimitExceededException(limit);
        }
    }

    /**
     * Raised when the stream grows past its limit.
     */
    public static class LimitExceededException extends IOException {
        public LimitExceededException(long limit) {
            super("Request body exceeds the limit of " + limit + " bytes");
        }
    }
}
