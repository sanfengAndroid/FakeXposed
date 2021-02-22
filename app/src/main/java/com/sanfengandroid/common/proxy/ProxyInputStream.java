/*
 * Copyright (c) 2021 FakeXposed by sanfengAndroid.
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *  
 */

package com.sanfengandroid.common.proxy;

import java.io.IOException;
import java.io.InputStream;

public class ProxyInputStream extends InputStream {
    private final byte[] input;
    private final int size;
    private int index = 0;

    public ProxyInputStream(String input) {
        if (input == null) {
            this.input = null;
            this.size = 0;
        } else {
            this.input = input.getBytes();
            this.size = this.input.length;
        }
    }

    @Override
    public int read() throws IOException {
        if (index < size) {
            return input[index++];
        }
        return -1;
    }
}
