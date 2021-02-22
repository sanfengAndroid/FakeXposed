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
import java.io.OutputStream;

public class ProxyOutStream extends OutputStream {
    private final byte[] out;
    private final int size;
    private final OutputStream orig;

    private int index = 0;

    public ProxyOutStream(OutputStream orig, String out) {
        if (out == null) {
            this.out = null;
            this.size = 0;
        } else {
            this.out = out.getBytes();
            this.size = this.out.length;
        }
        this.orig = orig;
    }


    @Override
    public void write(int b) throws IOException {
        if (index < size) {
            orig.write(out[index++]);
        }
    }
}
