
package org.brewchain.evm.solidity.compiler;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Scanner;

/**
 * Created by Anton Nashatyrev on 03.03.2016.
 */
public class Solc {

    private File solc = null;

    Solc() {
        try {
            init();
        } catch (IOException e) {
            throw new RuntimeException("Can't init solc compiler: ", e);
        }
    }

    private void init() throws IOException {
//    		String customSolcPath = null;
//        if (customSolcPath != null) {
//            solc = new File(customSolcPath);
//            if (!solc.canExecute()) {
//                throw new RuntimeException(String.format(
//                        "Solidity compiler from config solc.path: %s is not a valid executable",customSolcPath
//                ));
//            }
//        } else {
            initBundled();
//        }
    }

    private void initBundled() throws IOException {
        File tmpDir = new File(System.getProperty("java.io.tmpdir"), "solc");
        tmpDir.mkdirs();

        InputStream is = getClass().getResourceAsStream("/native/" + getOS() + "/solc/file.list");
    		Scanner scanner = new Scanner(is);
        while (scanner.hasNext()) {
            String s = scanner.next();
            File targetFile = new File(tmpDir, s);
            System.out.println(targetFile.getPath());
            InputStream fis = getClass().getResourceAsStream("/native/" + getOS() + "/solc/" + s);
            Files.copy(fis, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            if (solc == null) {
                // first file in the list denotes executable
                solc = targetFile;
                solc.setExecutable(true);
            }
            targetFile.deleteOnExit();
        }
    }

    private String getOS() {
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.contains("win")) {
            return "win";
        } else if (osName.contains("linux")) {
            return "linux";
        } else if (osName.contains("mac")) {
            return "mac";
        } else {
            throw new RuntimeException("Can't find solc compiler: unrecognized OS: " + osName);
        }
    }

    public File getExecutable() {
        return solc;
    }
}
