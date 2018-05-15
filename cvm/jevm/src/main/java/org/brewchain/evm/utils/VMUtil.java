package org.brewchain.evm.utils;

import static java.lang.String.format;
import static org.apache.commons.codec.binary.Base64.encodeBase64String;
import static org.brewchain.evm.solidity.compiler.SolidityCompiler.Options.ABI;
import static org.brewchain.evm.solidity.compiler.SolidityCompiler.Options.BIN;
import static org.brewchain.evm.solidity.compiler.SolidityCompiler.Options.INTERFACE;
import static org.brewchain.evm.solidity.compiler.SolidityCompiler.Options.METADATA;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

import org.apache.commons.lang3.StringUtils;
import org.brewchain.cvm.pbgens.Cvm.PMContract;
import org.brewchain.cvm.pbgens.Cvm.PRetBuild;
import org.brewchain.evm.call.CallTransaction;
import org.brewchain.evm.solidity.compiler.CompilationResult;
import org.brewchain.evm.solidity.compiler.SolidityCompiler;
import org.fc.brewchain.bcapi.EncAPI;
import org.fc.brewchain.bcapi.KeyPairs;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class VMUtil {

	public static void solidCompoler(EncAPI encAPI,PRetBuild.Builder ret,byte[] source) throws IOException{
		
		SolidityCompiler.Result res = SolidityCompiler.compile(source, true, ABI, BIN, INTERFACE, METADATA);
		
		if (StringUtils.isNotBlank(res.errors) || StringUtils.isBlank(res.output)) {
			ret.setRetCode(-1);
			ret.setRetMessage(res.errors);
			log.error("res.errors：：" + res.errors);
		} else {
			// IOException
			CompilationResult result = VMUtil.parse(res.output);
			if (result.contracts != null && result.contracts.size() > 0) {
				ret.setRetCode(0);
				ret.setRetMessage("");
				for (String name : result.contracts.keySet()) {
					PMContract.Builder c = PMContract.newBuilder();
					c.setName(name);
					CompilationResult.ContractMetadata cm = result.contracts.get(name);

					KeyPairs key = encAPI.genKeys();
					
					c.setAddr(key.getAddress());
					c.setBin(cm.bin);
					c.setAbi(cm.abi);
					c.setMetadata(cm.metadata);

					CallTransaction.Contract contract = new CallTransaction.Contract(cm.abi);
					if (contract.functions != null && contract.functions.length > 0) {
						for (int i = 0; i < contract.functions.length; i++) {
							System.out.println("contract.functions[" + i + "]:「" + contract.functions[i].toString() + "」");
							c.addFunName(contract.functions[i].toString());
						}
					}

					// Abi abi = Abi.fromJson(cm.abi);
					// Entry onlyFunc = abi.get(0);
					// System.out.println();
					// if(onlyFunc.type == Type.function){
					// onlyFunc.inputs.size();
					// onlyFunc.outputs.size();
					// onlyFunc.constant;
					// }

					ret.addInfo(c);
				}
			} else {
				ret.setRetCode(0);
				ret.setRetMessage("没有找到合约");
			}
		}
	}
	
	public static CompilationResult parse(String rawJson) throws IOException {
		CompilationResult result = CompilationResult.parse(rawJson);
		return result;
	}
	
	
	
	
	
	public static String zipAndEncode(String content) {
        try {
            return encodeBase64String(compress(content));
        } catch (Exception e) {
            log.error("Cannot zip or encode: ", e);
            return content;
        }
    }
	
    private static final int BUF_SIZE = 4096;
    
    private static void write(InputStream in, OutputStream out, int bufSize) throws IOException {
        try {
            byte[] buf = new byte[bufSize];
            for (int count = in.read(buf); count != -1; count = in.read(buf)) {
                out.write(buf, 0, count);
            }
        } finally {
            closeQuietly(in);
            closeQuietly(out);
        }
    }
    public static void closeQuietly(Closeable closeable) {
        try {
            if (closeable != null) {
                closeable.close();
            }
        } catch (IOException ioe) {
            // ignore
        }
    }
	public static byte[] compress(byte[] bytes) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        ByteArrayInputStream in = new ByteArrayInputStream(bytes);
        DeflaterOutputStream out = new DeflaterOutputStream(baos, new Deflater(), BUF_SIZE);

        write(in, out, BUF_SIZE);

        return baos.toByteArray();
    }

    public static byte[] compress(String content) throws IOException {
        return compress(content.getBytes("UTF-8"));
    }
    
    
    
    
    
    
    
    public static void saveProgramTraceFile(String txHash, String content) {
        File file = createProgramTraceFile(txHash);
        if (file != null) {
            writeStringToFile(file, content);
        }
    }
    private static File createProgramTraceFile(String txHash) {
        File result = null;
        //TODO 配置参数
        if (false) {
            File file = new File(new File("/Users/ailen/evm/db", "/Users/ailen/evm/trace"), txHash + ".json");
            if (file.exists()) {
                if (file.isFile() && file.canWrite()) {
                    result = file;
                }
            } else {
                try {
                    file.getParentFile().mkdirs();
                    file.createNewFile();
                    result = file;
                } catch (IOException e) {
                    // ignored
                }
            }
        }

        return result;
    }
    private static void writeStringToFile(File file, String data) {
        OutputStream out = null;
        try {
            out = new FileOutputStream(file);
            if (data != null) {
                out.write(data.getBytes("UTF-8"));
            }
        } catch (Exception e){
            log.error(format("Cannot write to file '%s': ", file.getAbsolutePath()), e);
        } finally {
            closeQuietly(out);
        }
    }
	
}
