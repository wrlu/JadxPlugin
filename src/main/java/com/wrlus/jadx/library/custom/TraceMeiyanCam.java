package com.wrlus.jadx.library.custom;

import com.wrlus.jadx.library.LibraryEntry;

import jadx.api.JadxArgs;
import jadx.api.JadxDecompiler;
import jadx.api.JavaClass;
import jadx.api.JavaMethod;
import jadx.api.plugins.input.data.ICodeReader;
import jadx.api.plugins.input.insns.InsnData;
import jadx.api.plugins.input.insns.Opcode;
import jadx.core.dex.nodes.MethodNode;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.function.Consumer;

public class TraceMeiyanCam implements LibraryEntry {
	static final String apkFilePath = "/home/xiaolu/Desktop/安全研究/应用安全/Camera逆向/美颜相机/com.meitu.meiyancamera_12.1.40.apk";
	static final String outputFilePath = "/home/xiaolu/Desktop/安全研究/应用安全/Camera逆向/美颜相机/trace_number.map";

	@Override
	public void onLibraryLoaded(String[] args) {
		File outputFile = new File(outputFilePath);
		if (outputFile.exists()) {
			System.out.println(outputFilePath + " exists and delete it, status: "
					+ outputFile.delete());
		}
		JadxArgs jadxArgs = new JadxArgs();
		jadxArgs.setInputFile(new File(apkFilePath));
		try (JadxDecompiler jadx = new JadxDecompiler(jadxArgs)) {
			jadx.load();
			for (JavaClass cls : jadx.getClassesWithInners()) {
				if (classFilter(cls.getFullName())) {
					continue;
				}
				for (JavaMethod mth : cls.getMethods()) {
					MethodNode methodNode = mth.getMethodNode();
					ICodeReader codeReader = methodNode.getCodeReader();
					if (codeReader != null) {
						codeReader.visitInstructions(new FindTraceIntConsumer(mth.getFullName()));
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static boolean classFilter(String fullName) {
		return fullName.startsWith("android") ||
				fullName.startsWith("com.google") ||
				fullName.startsWith("com.airbnb") ||
				fullName.startsWith("com.alibaba") ||
				fullName.startsWith("com.baidu") ||
				fullName.startsWith("com.bumptech") ||
				fullName.startsWith("com.bytedance") ||
				fullName.startsWith("com.getui") ||
				fullName.startsWith("com.heytap") ||
				fullName.startsWith("com.huawei") ||
				fullName.startsWith("com.meizu") ||
				fullName.startsWith("com.oplus") ||
				fullName.startsWith("com.qiniu") ||
				fullName.startsWith("com.qq") ||
				fullName.startsWith("com.ss.android") ||
				fullName.startsWith("com.tencent") ||
				fullName.startsWith("com.vivo") ||
				fullName.startsWith("com.xiaomi");
	}

	static class FindTraceIntConsumer implements Consumer<InsnData> {
		private int insnCount;
		private int constOpcodeOffset;
		private final String fullMethodName;
		private long traceNumber;

		public FindTraceIntConsumer(String fullMethodName) {
			this.fullMethodName = fullMethodName;
			insnCount = 0;
			constOpcodeOffset = -1;
			traceNumber = 0;
		}

		@Override
		public void accept(InsnData insnData) {
			insnData.decode();
			Opcode opcode = insnData.getOpcode();
			// Find the first CONST opcode.
			if (opcode == Opcode.CONST && constOpcodeOffset == -1) {
				traceNumber = insnData.getLiteral();
				constOpcodeOffset = insnCount;
			} else if (insnCount == constOpcodeOffset + 1) {
				// The next opcode after CONST should be INVOKE_STATIC or INVOKE_STATIC_RANGE.
				if ((opcode == Opcode.INVOKE_STATIC || opcode == Opcode.INVOKE_STATIC_RANGE) && traceNumber > 0) {
					writeResultToFile(traceNumber);
					System.out.println("Found trace number for method: " + fullMethodName);
				}
			}
			++insnCount;
		}

		private void writeResultToFile(long traceNumber) {
			try {
				FileWriter fw = new FileWriter(outputFilePath, true);
				fw.write(fullMethodName + "," + traceNumber + "\n");
				fw.flush();
				fw.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
