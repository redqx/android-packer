package com.luoye.dpt.builder;

import com.android.apksigner.ApkSignerTool;
import com.iyxan23.zipalignjava.ZipAlign;
import com.luoye.dpt.Const;
import com.luoye.dpt.elf.ReadElf;
import com.luoye.dpt.model.Instruction;
import com.luoye.dpt.model.MultiDexCode;
import com.luoye.dpt.task.ThreadPool;
import com.luoye.dpt.util.DexUtils;
import com.luoye.dpt.util.FileUtils;
import com.luoye.dpt.util.IoUtils;
import com.luoye.dpt.util.LogUtils;
import com.luoye.dpt.util.ManifestUtils;
import com.luoye.dpt.util.MultiDexCodeUtils;
import com.luoye.dpt.util.RC4Utils;
import com.luoye.dpt.util.ZipUtils;
import com.wind.meditor.core.FileProcesser;
import com.wind.meditor.property.AttributeItem;
import com.wind.meditor.property.ModificationProperty;
import com.wind.meditor.utils.NodeValue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Apk extends AndroidPackage {

    private boolean dumpCode;

    public static class Builder extends AndroidPackage.Builder {
        private boolean dumpCode;
        @Override
        public Apk build() {
            return new Apk(this);
        }

        public Builder filePath(String path) {
            this.filePath = path;
            return this;
        }

        public Builder packageName(String packageName) {
            this.packageName = packageName;
            return this;
        }

        public Builder debuggable(boolean debuggable) {
            this.debuggable = debuggable;
            return this;
        }

        public Builder sign(boolean sign) {
            this.sign = sign;
            return this;
        }

        public Builder dumpCode(boolean dumpCode) {
            this.dumpCode = dumpCode;
            return this;
        }

        public Builder appComponentFactory(boolean appComponentFactory) {
            this.appComponentFactory = appComponentFactory;
            return this;
        }
    }

    protected Apk(Builder builder) {
        setFilePath(builder.filePath);
        setDebuggable(builder.debuggable);
        setAppComponentFactory(builder.appComponentFactory);
        setSign(builder.sign);
        setPackageName(builder.packageName);
        setDumpCode(builder.dumpCode);
    }

    public void setDumpCode(boolean dumpCode) {
        this.dumpCode = dumpCode;
    }

    public boolean isDumpCode() {
        return dumpCode;
    }

    private static void process(Apk apk)
    {
        if(!new File("shell-files").exists()) //相关配置文件
        {
            LogUtils.error("Cannot find shell files!");
            return;
        }
        File apkFile = new File(apk.getFilePath());

        if(!apkFile.exists())
        {
            LogUtils.error("Apk not exists!");
            return;
        }

        //apk extract path
        String apkMainProcessPath_WORK_DIR = apk.getWorkspaceDir().getAbsolutePath();//一个临时的工作目录,比如是C:\Users\tinyx\AppData\Local\Temp\dptOut

        LogUtils.info("Apk main process path: " + apkMainProcessPath_WORK_DIR);

        ZipUtils.unZip(apk.getFilePath(),apkMainProcessPath_WORK_DIR);//把文件解压到apkMainProcessPath
        String packageName = ManifestUtils.getPackageName(apkMainProcessPath_WORK_DIR + File.separator + "AndroidManifest.xml");
        apk.setPackageName(packageName);
        apk.extractDexCode(apkMainProcessPath_WORK_DIR);//函数抽取,并把原始函数抽取的字节码相关信息问价存放于asset/OoooooOooo中

        apk.addJunkCodeDex(apkMainProcessPath_WORK_DIR);//把junkcode.dex添加到目录中,重命名为xxx.dex,比如classes4.dex
        apk.compressDexFiles(apkMainProcessPath_WORK_DIR);//把之前所有的dex文件,包含junkcode功能的dex文件一并打包为zip,存放于asset/i11111i111.zip中
        apk.deleteAllDexFiles(apkMainProcessPath_WORK_DIR);//删除原位置所有的dex文件, 在前面我们以及把dex备份为asset/i11111i111.zip

        apk.saveApplicationName(apkMainProcessPath_WORK_DIR);//从AndroidManifest.xml中读取Application对象的名称,然后以文本的形式写入到asset/app_name文件中
        apk.writeProxyAppName(apkMainProcessPath_WORK_DIR);//修改AndroidManifest.xml,写入代理Application,为壳的com.luoyesiqiu.shell.ProxyApplication
        if(apk.isAppComponentFactory()){//-c,--disable-acf      Disable app component factory(just use for debug). 这个是由我们输入的cmd指令缺点的,默认是true
            apk.saveAppComponentFactory(apkMainProcessPath_WORK_DIR);//appComponentFactory 是一个用于创建应用程序组件的工厂类。它允许开发者自定义应用程序组件的创建过程, //从AndroidManifest.xml中读取android:appComponentFactory="xxxx",然偶把xxxx文本写入asset/app_acf
            apk.writeProxyComponentFactoryName(apkMainProcessPath_WORK_DIR);//修改AndroidManifest.xml,写入代理ComponentFactory,为壳的"com.luoyesiqiu.shell.ProxyComponentFactory"
        }
        if(apk.isDebuggable()) {//cmd选项,-D,--debug            Make apk debuggable. 让app变得可调试
            LogUtils.info("Make apk debuggable.");
            apk.setDebuggable(apkMainProcessPath_WORK_DIR, true);//在AndroidManifest.xml中设置android:debuggable="true"
        }

        apk.setExtractNativeLibs(apkMainProcessPath_WORK_DIR);//好像往AndroidManifest.xml中写入android:extractNativeLibs="true"

        apk.addProxyDex(apkMainProcessPath_WORK_DIR);//往目录写入代理的dex:classes.dex
        apk.copyNativeLibs(apkMainProcessPath_WORK_DIR);//把需要用到的so文件,复制到asset/vwwwwwvwww/目录下
        apk.encryptSoFiles(apkMainProcessPath_WORK_DIR);//对asset/vwwwwwvwww/下,对所有含有.bitcode节的so进行节区解密处理,加密.bitcode节. 采用RC4算法

        apk.buildApk(apkFile.getAbsolutePath(),apkMainProcessPath_WORK_DIR, FileUtils.getExecutablePath());//对新的apk进行签名,涉及压缩打包,压缩优化,最终v1,v2,v3签名

        //删除工作目录
        File apkMainProcessFile = new File(apkMainProcessPath_WORK_DIR);
        if (apkMainProcessFile.exists()) {
            FileUtils.deleteRecurse(apkMainProcessFile);
        }
        LogUtils.info("All done.");
    }
    public void protect() {
        process(this);
    }

    private String getUnsignApkName(String apkName){
        return FileUtils.getNewFileName(apkName,"unsign");
    }

    private String getUnzipalignApkName(String apkName){
        return FileUtils.getNewFileName(apkName,"unzipalign");
    }

    private String getSignedApkName(String apkName){
        return FileUtils.getNewFileName(apkName,"signed");
    }

    /**
     * Write proxy ApplicationName
     */
    private void writeProxyAppName(String filePath){
        String inManifestPath = filePath + File.separator + "AndroidManifest.xml";
        String outManifestPath = filePath + File.separator + "AndroidManifest_new.xml";
        ManifestUtils.writeApplicationName(inManifestPath,outManifestPath, Const.PROXY_APPLICATION_NAME);//会自动新建一个AndroidManifest.xml(AndroidManifest_new.xml),并写入壳的代理Application
        //往AndroidManifest.xml中写入代理的android:name="com.luoyesiqiu.shell.ProxyApplication"
        File inManifestFile = new File(inManifestPath);
        File outManifestFile = new File(outManifestPath);

        inManifestFile.delete();//删除原有的xml

        outManifestFile.renameTo(inManifestFile);//重命名xml
    }

    private void writeProxyComponentFactoryName(String filePath){
        String inManifestPath = filePath + File.separator + "AndroidManifest.xml";
        String outManifestPath = filePath + File.separator + "AndroidManifest_new.xml";
        ManifestUtils.writeAppComponentFactory(inManifestPath,outManifestPath, Const.PROXY_COMPONENT_FACTORY);//和之前操作一样,往AndroidManifest.xml中写入代理的android:appComponentFactory="com.luoyesiqiu.shell.ProxyComponentFactory"

        File inManifestFile = new File(inManifestPath);
        File outManifestFile = new File(outManifestPath);

        inManifestFile.delete();

        outManifestFile.renameTo(inManifestFile);
    }

    private void setExtractNativeLibs(String filePath){

        String inManifestPath = filePath + File.separator + "AndroidManifest.xml";
        String outManifestPath = filePath + File.separator + "AndroidManifest_new.xml";
        ModificationProperty property = new ModificationProperty();

        property.addApplicationAttribute(new AttributeItem(NodeValue.Application.EXTRACTNATIVELIBS, "true"));

        FileProcesser.processManifestFile(inManifestPath, outManifestPath, property);

        File inManifestFile = new File(inManifestPath);
        File outManifestFile = new File(outManifestPath);

        inManifestFile.delete();

        outManifestFile.renameTo(inManifestFile);
    }

    private void setDebuggable(String filePath,boolean debuggable){
        String inManifestPath = filePath + File.separator + "AndroidManifest.xml";
        String outManifestPath = filePath + File.separator + "AndroidManifest_new.xml";
        ManifestUtils.writeDebuggable(inManifestPath,outManifestPath, debuggable ? "true" : "false");

        File inManifestFile = new File(inManifestPath);
        File outManifestFile = new File(outManifestPath);

        inManifestFile.delete();

        outManifestFile.renameTo(inManifestFile);
    }

    private File getWorkspaceDir(){
        return FileUtils.getDir(Const.ROOT_OF_OUT_DIR,"dptOut");
    }

    /**
     * Get last process（zipalign，sign）dir
     */
    private File getLastProcessDir(){
        return FileUtils.getDir(Const.ROOT_OF_OUT_DIR,"dptLastProcess");
    }

    private File getOutAssetsDir(String filePath){
        return FileUtils.getDir(filePath,"assets");
    }

    private void addProxyDex(String apkDir){
        String proxyDexPath = "shell-files/dex/classes.dex";
        addDex(proxyDexPath,apkDir);
    }

    protected void addJunkCodeDex(String apkDir) {
        String junkCodeDexPath = "shell-files/dex/junkcode.dex";
        addDex(junkCodeDexPath,apkDir);
    }

    private void compressDexFiles(String apkDir){
        ZipUtils.compress(getDexFiles(apkDir),getOutAssetsDir(apkDir).getAbsolutePath() + File.separator + "i11111i111.zip");
    }

    private void copyNativeLibs(String apkDir){
        File file = new File(FileUtils.getExecutablePath(), "shell-files/libs");
        FileUtils.copy(file.getAbsolutePath(),getOutAssetsDir(apkDir).getAbsolutePath() + File.separator + "vwwwwwvwww");
    }

    private void encryptSoFiles(String apkDir){

        File obfDir = new File(getOutAssetsDir(apkDir).getAbsolutePath() + File.separator, "vwwwwwvwww");
        File[] soAbiDirs = obfDir.listFiles();//拿到asset/vwwwwwvwww/下的所有File对象
        if(soAbiDirs != null) {
            for (File soAbiDir : soAbiDirs) {
                File[] soFiles = soAbiDir.listFiles();
                if(soFiles != null) {
                    for (File soFile : soFiles) {
                        if(!soFile.getAbsolutePath().endsWith(".so")) {
                            continue;
                        }//只对so文件进行炒作
                        try {
                            ReadElf readElf = new ReadElf(soFile);
                            List<ReadElf.SectionHeader> sectionHeaders = readElf.getSectionHeaders();//拿到所有的section Header信息
                            readElf.close();
                            for (ReadElf.SectionHeader sectionHeader : sectionHeaders) {

                                if(".bitcode".equals(sectionHeader.getName())) {

                                    LogUtils.info("start encrypt %s section: %s,offset: %s,size: %s",
                                            soFile.getAbsolutePath(),
                                            sectionHeader.getName(),
                                            Long.toHexString(sectionHeader.getOffset()),
                                            Long.toHexString(sectionHeader.getSize())
                                    );

                                    byte[] bitcode = IoUtils.readFile(soFile.getAbsolutePath(),sectionHeader.getOffset(),(int)sectionHeader.getSize());//拿到指定节区的字节码

                                    byte[] enc = RC4Utils.crypt(Const.DEFAULT_RC4_KEY.getBytes(StandardCharsets.UTF_8),bitcode);//利用RC4加密

                                    IoUtils.writeFile(soFile.getAbsolutePath(),enc,sectionHeader.getOffset());//然后写入
                                }
                            }
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
             }
        }

    }

    private void deleteAllDexFiles(String dir){
        List<File> dexFiles = getDexFiles(dir);
        for (File dexFile : dexFiles) {
            dexFile.delete();
        }
    }

    private void addDex(String dexFilePath,String apkDir){
        File dexFile = new File(dexFilePath);
        List<File> dexFiles = getDexFiles(apkDir);
        int newDexNameNumber = dexFiles.size() + 1;
        String newDexPath = apkDir + File.separator + "classes.dex";
        if(newDexNameNumber > 1) {
            newDexPath = apkDir + File.separator + String.format(Locale.US, "classes%d.dex", newDexNameNumber);
        }
        byte[] dexData = IoUtils.readFile(dexFile.getAbsolutePath());
        IoUtils.writeFile(newDexPath,dexData);
    }

    private static String getManifestFilePath(String apkOutDir){
        return apkOutDir + File.separator + "AndroidManifest.xml";
    }

    private void saveApplicationName(String apkOutDir){
        String androidManifestFile = getManifestFilePath(apkOutDir);

        File appNameOutFile = new File(getOutAssetsDir(apkOutDir),"app_name");//传递进去的是一个(File parent,String Child)
        String appName = ManifestUtils.getApplicationName(androidManifestFile);

        appName = appName == null ? "" : appName;

        IoUtils.writeFile(appNameOutFile.getAbsolutePath(),appName.getBytes());
    }

    private void saveAppComponentFactory(String apkOutDir){//从AndroidManifest.xml中读取android:appComponentFactory="xxxx",然偶把xxxx文本写入asset/app_acf
        String androidManifestFile = getManifestFilePath(apkOutDir);
        File appNameOutFile = new File(getOutAssetsDir(apkOutDir),"app_acf");
        String appName = ManifestUtils.getAppComponentFactory(androidManifestFile);

        appName = appName == null ? "" : appName;

        IoUtils.writeFile(appNameOutFile.getAbsolutePath(),appName.getBytes());
    }

    private boolean isSystemComponentFactory(String name){
        if(name.equals("androidx.core.app.CoreComponentFactory") || name.equals("android.support.v4.app.CoreComponentFactory")){
            return true;
        }
        return false;
    }

    /**
     * Get dex file number
     * ex：classes2.dex return 1
     */
    private int getDexNumber(String dexName){
        Pattern pattern = Pattern.compile("classes(\\d*)\\.dex$");
        Matcher matcher = pattern.matcher(dexName);
        if(matcher.find()){
            String dexNo = matcher.group(1);
            return (dexNo == null || "".equals(dexNo)) ? 0 : Integer.parseInt(dexNo) - 1;
        }
        else{
            return  -1;
        }
    }

    private void  extractDexCode(String apkOutDir)
    {
        List<File> dexFiles = getDexFiles(apkOutDir);//从解压缩的apk目录中,获取所有的.dex文件的File对象
        Map<Integer,List<Instruction>> instructionMap = new HashMap<>();
        String appNameNew = "OoooooOooo";//大概是存放原始的函数的字节码
        String dataOutputPath = getOutAssetsDir(apkOutDir).getAbsolutePath() + File.separator + appNameNew;

        CountDownLatch countDownLatch = new CountDownLatch(dexFiles.size());//传入的是文件个数,不是文件的大小
        for(File dexFile : dexFiles)
        {
            ThreadPool.getInstance().execute(() ->
            {
                final int dexNo = getDexNumber(dexFile.getName());
                if(dexNo < 0){
                    return;
                }
                String extractedDexName = dexFile.getName().endsWith(".dex") ? dexFile.getName().replaceAll("\\.dex$", "_extracted.dat") : "_extracted.dat";
                File extractedDexFile = new File(dexFile.getParent(), extractedDexName);

                //遍历所有ClassDef，并遍历其中的所有函数，再调用extractMethod对单个函数进行处理。
                List<Instruction> ret = DexUtils.extractAllMethods(dexFile, extractedDexFile, getPackageName(), isDumpCode());
                instructionMap.put(dexNo,ret);

                File dexFileRightHashes = new File(dexFile.getParent(),FileUtils.getNewFileSuffix(dexFile.getName(),"dat"));
                DexUtils.writeHashes(extractedDexFile,dexFileRightHashes);
                dexFile.delete();
                extractedDexFile.delete();
                dexFileRightHashes.renameTo(dexFile);
                countDownLatch.countDown();
            });

//                final int dexNo = getDexNumber(dexFile.getName());
//                if(dexNo < 0){
//                    return;
//                }
//                String extractedDexName = dexFile.getName().endsWith(".dex") ? dexFile.getName().replaceAll("\\.dex$", "_extracted.dat") : "_extracted.dat";
//                String t1=dexFile.getParent();
//                File extractedDexFile = new File(t1,extractedDexName);//新的临时dex文件(被函数抽取的)
//
//                //遍历所有ClassDef，并遍历其中的所有函数，再调用extractMethod对单个函数进行处理。
//                List<Instruction> ret = DexUtils.extractAllMethods(dexFile, extractedDexFile, getPackageName(), isDumpCode());
//                instructionMap.put(dexNo,ret);
//                //对应的指令大小要x2才是这里的大小
//                File dexFileRightHashes = new File(dexFile.getParent(),FileUtils.getNewFileSuffix(dexFile.getName(),"dat"));//创建临时文件xxxx.dat,用于存放被处理的dex文件,最后重命名它为xxx.dex,替换原来的dex文件
//                DexUtils.writeHashes(extractedDexFile,dexFileRightHashes);//把tmp.dex缓存写入org.dex缓存, 貌似会自动修复dex的头部信息
//                dexFile.delete();//删除原始的org.dex
//                extractedDexFile.delete();//删除tmp.dex
//                dexFileRightHashes.renameTo(dexFile);//把原始的函数字节码填充为0,好像就没啥操作了


        }

        ThreadPool.getInstance().shutdown();

        try {
            countDownLatch.await();
        }
        catch (Exception ignored)
        {

        }

        MultiDexCode multiDexCode = MultiDexCodeUtils.makeMultiDexCode(instructionMap);
        MultiDexCodeUtils.writeMultiDexCode(dataOutputPath,multiDexCode);

    }
    /**
     * Get all dex files
     */
    private List<File> getDexFiles(String dir){
        List<File> dexFiles = new ArrayList<>();
        File dirFile = new File(dir);
        File[] files = dirFile.listFiles();
        if(files != null) {
            Arrays.stream(files).filter(it -> it.getName().endsWith(".dex")).forEach(dexFiles::add);
        }
        return dexFiles;
    }

    private void buildApk(String originApkPath,String unpackFilePath,String savePath) {

        String originApkName = new File(originApkPath).getName();
        String apkLastProcessDir = getLastProcessDir().getAbsolutePath();

        String unzipalignApkPath = savePath + File.separator + getUnzipalignApkName(originApkName);
        ZipUtils.zip(unpackFilePath, unzipalignApkPath);//先把之前工作目录下的apk数据压缩打包为app-debug_unzipalign.apk

        String keyStoreFilePath = apkLastProcessDir + File.separator + "debug.keystore";

        String keyStoreAssetPath = "assets/debug.keystore";
        //String keyStoreAssetPath = "./debug.keystore";
        try {
            ZipUtils.readResourceFromRuntime(keyStoreAssetPath, keyStoreFilePath);//这个路径的文件可能需要我去提前准备
        }
        catch (IOException e){
            e.printStackTrace();
        }

        String unsignedApkPath = savePath + File.separator + getUnsignApkName(originApkName);
        boolean zipalignSuccess = false;
        try {
            zipalignApk(unzipalignApkPath, unsignedApkPath);//zip优化
            zipalignSuccess = true;
        } catch (Exception e) {
            e.printStackTrace();
        }

        String willSignApkPath = null;
        if (zipalignSuccess) {
            LogUtils.info("zipalign success.");
            willSignApkPath = unsignedApkPath;

        }
        else{
            LogUtils.error("warning: zipalign failed!");
            willSignApkPath = unzipalignApkPath;
        }

        boolean signResult = false;

        String signedApkPath = savePath + File.separator + getSignedApkName(originApkName);

        //最后签名
        if(isSign()) {//在cmd中配置, -x,--no-sign          Do not sign apk.
            signResult = signApkDebug(willSignApkPath, keyStoreFilePath, signedApkPath);//对apk签名,最后调用的是 com.android.apksigner.ApkSignerTool.main(commandArray);进行签名,v1,v2,v3都签上
        }
        //文件删除炒作
        File willSignApkFile = new File(willSignApkPath);
        File signedApkFile = new File(signedApkPath);
        File keyStoreFile = new File(keyStoreFilePath);
        File idsigFile = new File(signedApkPath + ".idsig");


        LogUtils.info("willSignApkFile: %s ,exists: %s",willSignApkFile.getAbsolutePath(),willSignApkFile.exists());
        LogUtils.info("signedApkFile: %s ,exists: %s",signedApkFile.getAbsolutePath(),signedApkFile.exists());

        String resultPath = signedApkFile.getAbsolutePath();
        if (!signedApkFile.exists() || !signResult) {
            resultPath = willSignApkFile.getAbsolutePath();
        }
        else{
            if(willSignApkFile.exists()){
                willSignApkFile.delete();
            }
        }

        if(zipalignSuccess) {
            File unzipalignApkFile = new File(unzipalignApkPath);
            try {
                Path filePath = Paths.get(unzipalignApkFile.getAbsolutePath());
                Files.deleteIfExists(filePath);
            }catch (Exception e){
                LogUtils.debug("unzipalignApkPath err = %s", e);
            }
        }

        if (idsigFile.exists()) {
            idsigFile.delete();
        }

        if (keyStoreFile.exists()) {
            keyStoreFile.delete();
        }
        LogUtils.info("protected apk output path: " + resultPath + "\n");
    }

    private static boolean signApkDebug(String apkPath, String keyStorePath, String signedApkPath) {
        if (signApk(apkPath, keyStorePath, signedApkPath,
                Const.KEY_ALIAS,
                Const.STORE_PASSWORD,
                Const.KEY_PASSWORD)) {
            return true;
        }
        return false;
    }

    private static boolean signApk(String apkPath, String keyStorePath, String signedApkPath,
                                   String keyAlias,
                                   String storePassword,
                                   String KeyPassword) {
        ArrayList<String> commandList = new ArrayList<>();

        commandList.add("sign");
        commandList.add("--ks");
        commandList.add(keyStorePath);
        commandList.add("--ks-key-alias");
        commandList.add(keyAlias);
        commandList.add("--ks-pass");
        commandList.add("pass:" + storePassword);
        commandList.add("--key-pass");
        commandList.add("pass:" + KeyPassword);
        commandList.add("--out");
        commandList.add(signedApkPath);
        commandList.add("--v1-signing-enabled");
        commandList.add("true");
        commandList.add("--v2-signing-enabled");
        commandList.add("true");
        commandList.add("--v3-signing-enabled");
        commandList.add("true");
        commandList.add(apkPath);

        int size = commandList.size();
        String[] commandArray = new String[size];
        commandArray = commandList.toArray(commandArray);

        try {
            ApkSignerTool.main(commandArray);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private static void zipalignApk(String inputApkPath, String outputApkPath) throws Exception{
        RandomAccessFile in = new RandomAccessFile(inputApkPath, "r");
        FileOutputStream out = new FileOutputStream(outputApkPath);
        ZipAlign.alignZip(in, out);
        IoUtils.close(in);
        IoUtils.close(out);
    }
}
