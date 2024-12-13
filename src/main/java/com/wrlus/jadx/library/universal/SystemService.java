package com.wrlus.jadx.library.universal;

import com.wrlus.jadx.library.LibraryEntry;

import jadx.api.JadxArgs;
import jadx.api.JadxDecompiler;
import jadx.api.JavaClass;
import jadx.api.JavaMethod;
import jadx.core.dex.nodes.MethodNode;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class SystemService implements LibraryEntry {
	private static final String defaultRomPath = "/home/xiaolu/RawContent/FileSystem/Android/Vivo/PD2364_UP1A.231005.007_compiler11051725";
	private static final String androidFrameworkPath = "/packages/android";
	private static final String serviceListPath = "/service_list.txt";
	private static final String binderServiceAidlPath = "/binder_service.txt";
	private static final String binderAnonymousAidlPath = "/binder_anonymous_aidl.txt";

	@Override
	public void onLibraryLoaded(String[] args) {
		String romPath = defaultRomPath;
		if (args.length > 1) {
			romPath = args[1];
		}
		processRom(romPath);
	}

	private void processRom(String romPath) {
		File fwkFile = new File(romPath, androidFrameworkPath);
		File serviceListFile = new File(romPath, serviceListPath);
		if (!fwkFile.exists()) {
			System.err.println("No such file or directory: " + fwkFile.getAbsolutePath());
			return;
		}
		if (!serviceListFile.exists()) {
			System.err.println("No such file or directory: " + serviceListFile.getAbsolutePath());
			return;
		}
		File[] frameworkJars = fwkFile.listFiles();
		if (frameworkJars == null) {
			System.err.println("Permission denied: " + fwkFile.getAbsolutePath());
			return;
		}
		List<String> serviceList = getServiceList(serviceListFile);
		for (File frameworkJar : frameworkJars) {
			try {
				processFrameworkJar(frameworkJar, serviceList, romPath);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private List<String> getServiceList(File serviceListFile) {
		try {
			BufferedReader br = new BufferedReader(new FileReader(serviceListFile));
			Stream<String> lines = br.lines();
			List<String> result = new ArrayList<>();
			lines.forEach(s -> {
				if (!(s.contains("Found ") && s.contains(" services:"))) {
					result.add(s);
				}
			});
			return result;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return new ArrayList<>();
	}

	private void processFrameworkJar(File frameworkJarFile, List<String> serviceList, String romPath) {
		JadxArgs jadxArgs = new JadxArgs();
		jadxArgs.setInputFile(frameworkJarFile);
		JadxDecompiler jadx = new JadxDecompiler(jadxArgs);
		jadx.load();
		for (JavaClass cls : jadx.getClassesWithInners()) {
			if (isAidlClass(cls)) {
				System.out.println(cls.getFullName());
				File outputFile;
				if (isServiceManagerAidl(cls, serviceList)) {
					outputFile = new File(romPath, binderServiceAidlPath);
				} else {
					outputFile = new File(romPath, binderAnonymousAidlPath);
				}
				writeToFile(cls, outputFile, true);
			}
		}
	}

	private boolean isAidlClass(JavaClass cls) {
		boolean hasDefault = false;
		JavaClass stubCls = null;
		List<JavaClass> innerClasses = cls.getInnerClasses();
		for (JavaClass innerClass : innerClasses) {
			if (innerClass.getName().equals("Default")) {
				hasDefault = true;
			} else if (innerClass.getName().equals("Stub")) {
				stubCls = innerClass;
			}
			if (hasDefault && stubCls != null) {
				break;
			}
		}
		if (hasDefault && stubCls != null) {
			JavaClass proxyCls = null;
			List<JavaClass> stubInnerClasses = stubCls.getInnerClasses();
			for (JavaClass innerClass : stubInnerClasses) {
				if (innerClass.getName().equals("Proxy")) {
					proxyCls = innerClass;
					break;
				}
			}
			return proxyCls != null;
		}
		return false;
	}

	private String getAllAidlMethodString(JavaClass cls) {
		StringBuilder dump = new StringBuilder();
		for (JavaMethod mth : cls.getMethods()) {
			MethodNode methodNode = mth.getMethodNode();
			dump.append(methodNode.toString());
			dump.append('\n');
		}
		return dump.toString();
	}

	private boolean isServiceManagerAidl(JavaClass cls, List<String> serviceList) {
		String fullName = cls.getFullName();
		for (String service : serviceList) {
			if (service.contains(fullName)) {
				return true;
			}
		}
		return false;
	}

	private void writeToFile(JavaClass cls, File outputFile, boolean append) {
		try {
			FileWriter fw = new FileWriter(outputFile, append);
			fw.write(cls.getFullName());
			fw.write('\n');
			fw.write(getAllAidlMethodString(cls));
			fw.write('\n');
			fw.flush();
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
