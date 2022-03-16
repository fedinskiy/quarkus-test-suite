package io.quarkus.ts.http.restclient.reactive;

import java.io.IOException;

public class BashUtils {
    public static Process run(String... params) throws IOException, InterruptedException {
        ProcessBuilder bash = new ProcessBuilder(params);
        Process process = bash.start();

        process.waitFor();
        return process;
    }

    public static String getSum(String path) throws IOException, InterruptedException {
        Process process = BashUtils.run("md5sum", path);
        String result = new String(process.getInputStream().readAllBytes()).split(" ")[0];
        System.out.println("Hash of " + path + " is " + result);
        return result;
    }

    public static void createFile(String path, String size) throws IOException, InterruptedException {
        BashUtils.run("truncate", "-s", size, path);
    }
}
