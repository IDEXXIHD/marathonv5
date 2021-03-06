/*******************************************************************************
 * Copyright 2016 Jalian Systems Pvt. Ltd.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.logging.Logger;

import net.sourceforge.marathon.javaagent.Platform;

public class ProcessLauncher {

    public static final Logger LOGGER = Logger.getLogger(ProcessLauncher.class.getName());

    private static class LaunchConfig {

        private String[] command;
        private String shell;

        public LaunchConfig(String[] params) {
            this.command = params;
            this.shell = getShell();
        }

        private String getShell() {
            if (Platform.getCurrent().is(Platform.WINDOWS)) {
                return "cmd.exe";
            } else {
                return "/bin/sh";
            }
        }

        public String[] getExecArgs() {
            String[] execArgs = new String[3];
            execArgs[0] = shell;
            execArgs[1] = shell.endsWith("sh") ? "-c" : "/c";

            StringBuilder command = new StringBuilder();
            boolean first = true;
            for (String cmd : this.command) {
                if (first) {
                    cmd = cmd.replace('/', '\\');
                    if (cmd.contains(" ")) {
                        cmd = '"' + cmd + '"';
                    }
                    first = false;
                }
                command.append(cmd).append(" ");
            }
            command.setLength(command.length() - 1);
            execArgs[2] = command.toString();
            if (Platform.getCurrent().is(Platform.WINDOWS)) {
                execArgs[2] = "\"" + command + "\"";
            } else {
                execArgs[2] = command.toString();
            }
            return execArgs;
        }

    }

    public static String launch(String... params) throws IOException {
        LaunchConfig config = new LaunchConfig(params);

        ProcessBuilder pb = new ProcessBuilder(config.getExecArgs());
        Process process = pb.start();
        InputStream output = process.getInputStream();
        return readAll(output);
    }

    private static String readAll(InputStream output) throws IOException {
        Reader r = new BufferedReader(new InputStreamReader(output));
        StringBuilder sb = new StringBuilder();
        char[] buf = new char[1024];
        int n;
        while ((n = r.read(buf, 0, 1024)) != -1) {
            sb.append(buf, 0, n);
        }
        return sb.toString().replaceAll("\r\n", "\n");
    }
}
