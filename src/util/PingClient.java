/*
 * Copyright 2013 Push Technology
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package util;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Arrays;

public class PingClient {

    public static void main(String[] args) throws IOException, InterruptedException {
        String host = args.length > 0 ? args[0] : "localhost";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 12345;
        System.out.println("Pinging "+host+":" + port);
        SocketChannel sc = SocketChannel.open(new InetSocketAddress(host, port));

        configure(sc);
        ByteBuffer bb = ByteBuffer.allocateDirect(4096);
        long times[] = new long[1000 * 1000];
        for (int i = -10000; i < times.length; i++) {
            long start = System.nanoTime();
            bb.position(0);
            bb.limit(32);
            sc.write(bb);


            bb.clear();
            sc.read(bb);
            long end = System.nanoTime();
            long err = System.nanoTime() - end;
            long time = end - start - err;
            if (i >= 0)
                times[i] = time;
        }
        close(sc);
        Arrays.sort(times);
        System.out.printf("RTT Latency for 1/50/99%%tile %.1f/%.1f/%.1f us%n",
                times[times.length / 100] / 1e3,
                times[times.length / 2] / 1e3,
                times[times.length - times.length / 100 - 1] / 1e3
        );
    }

    static void close(Closeable sc2) {
        if (sc2 != null) try {
            sc2.close();
        } catch (IOException ignored) {
        }
    }


    static void configure(SocketChannel sc) throws SocketException {
        sc.socket().setTcpNoDelay(true);
    }
}
