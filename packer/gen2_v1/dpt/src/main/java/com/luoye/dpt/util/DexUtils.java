package com.luoye.dpt.util;

import com.android.dex.ClassData;
import com.android.dex.ClassDef;
import com.android.dex.Code;
import com.android.dex.Dex;
import com.luoye.dpt.model.Instruction;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.*;

/**
 * @author luoyesiqiu
 */
public class DexUtils {
    /* The following are the rules for classes that do not extract */
    private static final String[] excludeRule = {
      "Landroid/.*",
      "Landroidx/.*",
      "Lcom/squareup/okhttp/.*",
      "Lokio/.*", "Lokhttp3/.*",
      "Lkotlin/.*",
      "Lcom/google/.*",
      "Lrx/.*",
      "Lorg/apache/.*",
      "Lretrofit2/.*",
      "Lcom/alibaba/.*",
      "Lcom/amap/api/.*",
      "Lcom/sina/weibo/.*",
      "Lcom/xiaomi/.*",
      "Lcom/eclipsesource/.*",
      "Lcom/blankj/utilcode/.*",
      "Lcom/umeng/.*",
      "Ljavax/.*",
      "Lorg/slf4j/.*"
    };

    /**
     * Extract all methods code
     * @param dexFile dex input path
     * @param outDexFile dex output path
     * @return insns list
     */
    public static List<Instruction> extractAllMethods(File dexFile, File outDexFile,String packageName,boolean dumpCode) {
        List<Instruction> instructionList = new ArrayList<>();
        Dex dex = null;
        RandomAccessFile randomAccessFile = null;
        byte[] dexData = IoUtils.readFile(dexFile.getAbsolutePath());//读取二进制的dex文件
        IoUtils.writeFile(outDexFile.getAbsolutePath(),dexData);//把原始的dex内容输出到xxx_extracted.dex中
        JSONArray dumpJSON = new JSONArray();
        try {
            dex = new Dex(dexFile);//Dex模块解析对象
            randomAccessFile = new RandomAccessFile(outDexFile, "rw");//任意的读取文件
            Iterable<ClassDef> classDefs = dex.classDefs();//返回的是对dex解析的内容
            for (ClassDef classDef : classDefs)
            {//取出当前dex涉及的所有class
                boolean skip = false;
                //Skip exclude classes name
                for(String rule : excludeRule)
                {//是否要跳过当前类的处理,可能一些敏感的类禁止去处理
                    if(classDef.toString().matches(rule)){
                        skip = true;
                        break;
                    }
                }
                if(skip){
                    continue;
                }
                if(classDef.getClassDataOffset() == 0)
                {
                    LogUtils.noisy("class '%s' data offset is zero",classDef.toString());
                    continue;
                }

                JSONObject classJSONObject = new JSONObject();
                JSONArray classJSONArray = new JSONArray();
                ClassData classData = dex.readClassData(classDef);//classData包含了一些信息,比如成员信息,函数信息

                String className = dex.typeNames().get(classDef.getTypeIndex());
                String humanizeTypeName = TypeUtils.getHumanizeTypeName(className);

                ClassData.Method[] directMethods = classData.getDirectMethods();
                ClassData.Method[] virtualMethods = classData.getVirtualMethods();
                for (ClassData.Method method : directMethods) {
                    Instruction instruction = extractMethod(dex,randomAccessFile,classDef,method);
                    if(instruction != null) {
                        instructionList.add(instruction);
                        putToJSON(classJSONArray, instruction);
                    }
                }

                for (ClassData.Method method : virtualMethods) {
                    Instruction org_instruction = extractMethod(dex, randomAccessFile,classDef, method);//返回原始的函数机器码的Instruction对象
                    if(org_instruction != null)
                    {
                        instructionList.add(org_instruction);
                        putToJSON(classJSONArray, org_instruction);
                    }
                }

                classJSONObject.put(humanizeTypeName,classJSONArray);
                dumpJSON.put(classJSONObject);
            }
        }
        catch (Exception e){

        }
        finally {
            IoUtils.close(randomAccessFile);
            if(dumpCode) //这个在命令行指令中配置,是否选择需要dump出这个JSON数据
            {
                //-d,--dump-code    Dump the code item of DEX and save it to .json files.
                dumpJSON(packageName,dexFile, dumpJSON);
            }
        }

        return instructionList;
    }

    private static void dumpJSON(String packageName, File originFile, JSONArray array){
        File pkg = new File(packageName);
        if(!pkg.exists()){
            pkg.mkdirs();
        }
        File writePath = new File(pkg.getAbsolutePath(),originFile.getName() + ".json");
        LogUtils.info("dump json to path: %s",writePath.getParentFile().getName() + File.separator + writePath.getName());

        IoUtils.writeFile(writePath.getAbsolutePath(),array.toString(1).getBytes());
    }

    private static void putToJSON(JSONArray array,Instruction instruction)
    {
        JSONObject jsonObject = new JSONObject();
        String hex = HexUtils.toHexArray(instruction.getInstructionsData());
        jsonObject.put("methodId",instruction.getMethodIndex());
        jsonObject.put("code",hex);
        array.put(jsonObject);
    }

    /**
     * Extract a method code
     * @param arg_dex dex struct
     * @param arg_fd_outFile out file
     * @param arg_method will extract method
     * @return a insns
     */
    private static Instruction extractMethod(Dex arg_dex ,RandomAccessFile arg_fd_outFile,ClassDef arg_classDef,ClassData.Method arg_method)
            throws Exception{
        String returnTypeName = arg_dex.typeNames().get(arg_dex.protoIds().get(arg_dex.methodIds().get(arg_method.getMethodIndex()).getProtoIndex()).getReturnTypeIndex());
        String methodName = arg_dex.strings().get(arg_dex.methodIds().get(arg_method.getMethodIndex()).getNameIndex());
        String className = arg_dex.typeNames().get(arg_classDef.getTypeIndex());
        //native function or abstract function
        if(arg_method.getCodeOffset() == 0)
        {//空函数,不处理
            LogUtils.noisy("method code offset is zero,name =  %s.%s , returnType = %s",
                    TypeUtils.getHumanizeTypeName(className),
                    methodName,
                    TypeUtils.getHumanizeTypeName(returnTypeName));
            return null;
        }
        Instruction org_instruction_info = new Instruction();
        //16 = registers_size + ins_size + outs_size + tries_size + debug_info_off + insns_size
        int insnsOffset = arg_method.getCodeOffset() + 16;
        Code code = arg_dex.readCode(arg_method);//返回当前函数的CodeItem结构体
        //Fault-tolerant handling
        if(code.getInstructions().length == 0)
        {
            LogUtils.noisy("method has no code,name =  %s.%s , returnType = %s",
                    TypeUtils.getHumanizeTypeName(className),
                    methodName,
                    TypeUtils.getHumanizeTypeName(returnTypeName));
            return null;
        }
        int insnsCapacity = code.getInstructions().length;//指令个数
        //The insns capacity is not enough to store the return statement, skip it
        byte[] returnByteCodes = getReturnByteCodes(returnTypeName);//获取 函数返回的相关的字节码
        if(insnsCapacity * 2 < returnByteCodes.length)
        {//除开返回指令,就没字节码了. 那也不处理.
            LogUtils.noisy("The capacity of insns is not enough to store the return statement. %s.%s() ClassIndex = %d -> %s insnsCapacity = %d byte(s) but returnByteCodes = %d byte(s)",
                    TypeUtils.getHumanizeTypeName(className),
                    methodName,
                    arg_classDef.getTypeIndex(),
                    TypeUtils.getHumanizeTypeName(returnTypeName),
                    insnsCapacity * 2,
                    returnByteCodes.length);

            return null;
        }
        org_instruction_info.setOffsetOfDex(insnsOffset);
        //Here, MethodIndex corresponds to the index of the method_ids area
        org_instruction_info.setMethodIndex(arg_method.getMethodIndex());
        //Note: Here is the size of the array
        org_instruction_info.setInstructionDataSize(insnsCapacity * 2);
        byte[] org_Asm_byteCode = new byte[insnsCapacity * 2];
        //Write nop instruction
        for (int i = 0; i < insnsCapacity; i++) //对指令个数进行依次处理
        {//把函数的所有指令都转化为nop(0000)
            arg_fd_outFile.seek(insnsOffset + (i * 2));
            org_Asm_byteCode[i * 2] = arg_fd_outFile.readByte();//读取原字节码
            org_Asm_byteCode[i * 2 + 1] = arg_fd_outFile.readByte();
            arg_fd_outFile.seek(insnsOffset + (i * 2));
            arg_fd_outFile.writeShort(0);//写入字节码0
        }
        org_instruction_info.setInstructionsData(org_Asm_byteCode);
        arg_fd_outFile.seek(insnsOffset);
        //Write return instruction
        arg_fd_outFile.write(returnByteCodes);//函数开头出写入 返回指令,

        return org_instruction_info;
    }

    /**
     * Obtain the code of the return statement based on the jvm type
     */
    public static byte[] getReturnByteCodes(String typeName){
        byte[] returnVoidCodes = {(byte)0x0e , (byte)(0x0)};
        byte[] returnCodes = {(byte)0x12 , (byte)0x0 , (byte) 0x0f , (byte) 0x0};
        byte[] returnWideCodes = {(byte)0x16 , (byte)0x0 , (byte) 0x0 , (byte) 0x0, (byte) 0x10 , (byte) 0x0};
        byte[] returnObjectCodes = {(byte)0x12 , (byte)0x0 , (byte) 0x11 , (byte) 0x0};
        switch (typeName){
            case "V":
                return returnVoidCodes;
            case "B":
            case "C":
            case "F":
            case "I":
            case "S":
            case "Z":
                return returnCodes;
            case "D":
            case "J":
                return returnWideCodes;
            default: {
                return returnObjectCodes;
            }
        }
    }

    /**
     * Write dex hashes
     */
    public static void writeHashes(File oldDexFile,File newDexFile){
        byte[] dexData = IoUtils.readFile(oldDexFile.getAbsolutePath());

        Dex dex = null;
        try {
            dex = new Dex(dexData);
            dex.writeHashes();
            dex.writeTo(newDexFile);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * Restore dex code to dex
     */
    public static void restoreInstructions(File dexFile,List<Instruction> instructions) throws IOException {
        Dex dex = new Dex(dexFile);
        RandomAccessFile randomAccessFile = new RandomAccessFile(dexFile,"rw");
        Iterable<ClassDef> classDefs = dex.classDefs();
        int listIndex = 0;
        for (ClassDef classDef : classDefs) {
            ClassData.Method[] methods = dex.readClassData(classDef).allMethods();
            for(int i = 0; i < methods.length ;i++){
                ClassData.Method method = methods[i];
                int offsetInstructions = method.getCodeOffset() + 16;
                Instruction instruction = instructions.get(listIndex ++ );
                if(instruction.getMethodIndex() == method.getMethodIndex()) {
                    byte[] byteCode = Base64.getDecoder().decode(instruction.getInstructionsData());

                    randomAccessFile.seek(offsetInstructions);
                    randomAccessFile.write(byteCode,0,byteCode.length);
                }
            }
        }

        randomAccessFile.close();
    }

}
