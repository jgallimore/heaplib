/**
 * Copyright 2019 Alexey Ragozin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.perfkit.heaplib.cli;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.management.ManagementFactory;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.perfkit.heaplib.cli.HeapCLI;

/**
 * JUnit command runner.
 *
 * @author Alexey Ragozin (alexey.ragozin@gmail.com)
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class CliCheck {

    private static String PID;
    static {
        PID = ManagementFactory.getRuntimeMXBean().getName();
        PID = PID.substring(0, PID.indexOf('@'));
    }

    private static void assume_64bit() {
        Assume.assumeTrue("Should run on 64bit VM", ManagementFactory.getRuntimeMXBean().getVmName().contains("64"));
    }

    public void copy(String path, String target) {
        if (new File(target).isFile()) {
            new File(target).delete();
        }
        if (new File(target).getParentFile() != null) {
            new File(target).getParentFile().mkdirs();
        }
        try {
            FileInputStream fis = new FileInputStream(path);
            FileOutputStream fos = new FileOutputStream(target);
            byte[] buf = new byte[16 << 10];
            while (true) {
                int n = fis.read(buf);
                if (n < 0) {
                    break;
                }
                fos.write(buf, 0, n);
            }
            fis.close();
            fos.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void help() {
        exec("--help");
    }

    @Test
    public void list_commands() {
        exec("--commands");
    }

    @Test
    public void histo() {
        exec("histo", "-d", "../hprof-oql-engine/src/test/resources/small_heap.bin");
    }

    @Test
    public void histo_index() {
        exec("histo", "-d", "../hprof-oql-engine/src/test/resources/small_heap.bin", "--index", "target/hprof.aux");
    }

    @Test
    public void histo_fast() {
        exec("histo", "--noindex", "-d", "../hprof-oql-engine/src/test/resources/small_heap.bin");
    }

    @Test
    public void histo_big_dump() {
        assume_64bit();
        exec("histo", "--noindex", "-d", "../dumps/dump1.live.hprof");
    }

    @Test
    public void histo_big_dump2() {
        assume_64bit();
        exec("histo", "-d", "../dumps/dump1.live.hprof", "-X");
    }

    @Test
    public void mask() {
        exec("mask", "--noindex", "-d", "../hprof-oql-engine/src/test/resources/small_heap.bin", "-s",
                "(**.Thread).name");
    }

    @Test
    public void mask_rex() {
        exec("mask", "--noindex", "-d", "../hprof-oql-engine/src/test/resources/small_heap.bin", "-s",
                "(**.Thread).name", "-m", ".*(Com).*(Thread).*");
    }

    @Test
    public void mask_string() {
        exec("mask", "--noindex", "-d", "../hprof-oql-engine/src/test/resources/small_heap.bin", "-s", "name", "-m",
                ".*(java).*");
    }

    @Test
    public void mask_string_patch() {
        String target = "target/tmp/small_heap_mask_string_patch.bin";
        copy("../hprof-oql-engine/src/test/resources/small_heap.bin", target);
        exec("mask", "--noindex", "--patch", "-d", target, "-s", "name", "-m", ".*(java).*");
        exec("mask", "--noindex", "-d", target, "-s", "name");
    }

    @Test
    public void mask_rex_patch() {
        String target = "target/tmp/small_heap_mask_rex_patch.bin";
        copy("../hprof-oql-engine/src/test/resources/small_heap.bin", target);
        exec("mask", "--noindex", "--patch", "-d", target, "-s", "(**.Thread).name", "-m", ".*(Com).*(Thread).*");
        exec("mask", "--noindex", "-d", target, "-s", "(**.Thread).name");
    }

    @Test
    public void mask_bin_rex() {
        assume_64bit();
        exec("mask", "--noindex", "-d", "../dumps/dump1.live.hprof", "-s", "(**.Thread).name", "-m", ".*serverUUID.*");
    }

    @Test
    public void mask_bin_rex_patch() {
        assume_64bit();
        exec("mask", "--noindex", "--patch", "-d", "../dumps/dump1.live.hprof", "-s", "(**.Thread).name", "-m",
                ".*serverUUID=([0-9a-z]+).*");
    }

    @Test
    public void grep() {
        exec("grep", "--noindex", "-d", "../hprof-oql-engine/src/test/resources/small_heap.bin", "-s", "name:.*(java).*", "(**.Thread).name", "-X");
    }

    @Test
    public void exec() {
        exec("exec", "--noindex", "-d", "../hprof-oql-engine/src/test/resources/small_heap.bin", "-e", "select a from java.lang.Thread a", "-X");
    }

    @Test
    public void exec2() {
        exec("exec", "--noindex", "-d", "../hprof-oql-engine/src/test/resources/small_heap.bin", "-e", "select {name: a.name, priority: a.priority} from java.lang.Thread a", "-X");
    }

    @Test
    public void exec_rwlock_dump_visvm() {
        exec("exec", "--noindex", "-X",
                "-d", "../hprof-oql-engine/src/test/resources/rwdeadlock.hprof",
                "-f", "../hprof-oql-engine/src/test/resources/oql/rwlock_dump_visvm.js");
    }

    @Test
    public void exec_rwlock_dump() {
        exec("exec", "--noindex", "-X",
                "-d", "../hprof-oql-engine/src/test/resources/rwdeadlock.hprof",
                "-f", "../hprof-oql-engine/src/test/resources/oql/rwlock_dump_console.js");
    }

    @Test
    public void exec_thread_dump() {
        exec("exec", "--index", "../blcdump.hprof.hwcache", "-X",
                "-d", "../dumps/blcdump.hprof",
                "-f", "../hprof-oql-engine/src/test/resources/oql/thread_dump_console.js");
    }

    @Test
    public void exec_test() {
        String script = "length(map(\n" +
                "    filter(\n" +
                "        heap.roots(),\n" +
                "        function(r) {\n" +
                "            return root(r) && root(r).type == \"thread object\";\n" +
                "        }\n" +
                "    ),\n" +
                "    function(t) {\n" +
                "    \n" +
                "        print(\"\\n#\" + t.id + \" \" + t.name.toString());\n" +
                "        Java.type(\"java.util.Arrays\").asList(root(t).wrapped.getStackTrace()).forEach(function(e){ \n" +
                "            print(\"  \" + e);\n" +
                "        }); \n" +
                "    }\n" +
                "))\n" +
                "\n" +
                "\"\""
                + "";

        exec("exec", "--index", "../dumps/blcdump.hprof.hwcache", "-X",
                "-d", "../dumps/blcdump.hprof",
                "-e", script);
    }

    @Test
    public void exec_test2() {
        String script = "map(heap.finalizables(), function(f) {return f.value});"
                + "";

        exec("exec", "--index", "../dumps/blcdump.hprof.hwcache", "-X",
                "-d", "../dumps/blcdump.hprof",
                "-e", script);
    }

    @Test
    public void exec_test3() {
        String script = "map(heap.classes(), function(f) {return f});"
                + "";

        exec("exec", "--index", "../dumps/blcdump.hprof.hwcache", "-X",
                "-d", "../dumps/blcdump.hprof",
                "-e", script);
    }

    private void exec(String... cmd) {
        HeapCLI sjk = new HeapCLI();
        sjk.suppressSystemExit();
        StringBuilder sb = new StringBuilder();
        sb.append("CLI");
        for (String c : cmd) {
            sb.append(' ').append(escape(c));
        }
        System.out.println(sb);
        Assert.assertTrue(sjk.start(cmd));
    }

    private Object escape(String c) {
        if (c.split("\\s").length > 1) {
            return '\"' + c + '\"';
        } else {
            return c;
        }
    }
}
