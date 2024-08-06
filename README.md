项目结构

```
├── analysis 		: 项目分析文章
├── asset 
├── example 		: 测试案例
├── packer 			: 加壳项目
├── README.md 
└── source 			: 待加壳项目
```



# 测试apk

项目 com.packer.source

```
source : generate Apk for testing
	- org_v1(com.packer.org_v1) =>  org_v1-debug.apk
```



# 加壳器

涉及的加壳器: 自动化实现apk解包,打包, AndroidManifest.xml修改, apk签名



## 一代| gen1



### gen1_v1

position => android-packer/packer/gen1_v1



简单的一代壳原理实现, 基于[dpt-shell](https://github.com/luoyesiqiu/dpt-shell) 基础上做一个修改. 主要是提取了其中的加壳模块

=>  [原理分析](./analysis/gen1_v1.md)



```
项目布局:
	-dpt: java项目, 用于加壳. 可以生成jar包
	-shell: Android Studio项目, 实现application代理, 动态加载dex
ps: Android studio也可以调试java项目, 比如调试单个java文件之类的
```



usage:

```
usage: java -jar dpt.jar [option] -f <apk>
 -D,--debug            Make apk debuggable.
 -l,--noisy-log        Open noisy log.
 -x,--no-sign          Do not sign apk.


λ java -jar dpt.jar -f org_v01.apk

ps: 之后会生成一个org_v01_signed.apk文件, 安装运行即可
```



> 不足之处: 

对于org_v1.apk的测试案例, 不知道为什么只能触发 Application的代理类执行, 不能触发AppComponentFactory代理类的执行

在有限的知识储备下,我认为AppComponentFactory和 Application类好像不能并发执行



### gen1_v2

position => android-packer/packer/gen1_v2

简单的一代壳原理实现, 基于[dpt-shell](https://github.com/luoyesiqiu/dpt-shell) 基础上做一个修改. 

在dpt-shell的大框架上, 剔出了函数抽取相关部分, 只关注于dex的加载与执行

=>  [原理分析](./analysis/gen1_v2.md), gen1_v2和gen1_v1的原理有一些关键的区别



```
项目布局:
	-dpt: java项目, 用于加壳,可以生成jar包
	-shell: Android Studio项目, 功能是加载dex, 可以调试来辅助理解
```



usage:

```
usage: java -jar dpt.jar [option] -f <apk>
 -c,--disable-acf      Disable app component factory(just use for debug).
 -D,--debug            Make apk debuggable.
 -f,--apk-file <arg>   Need to protect apk file.
 -l,--noisy-log        Open noisy log.
 -x,--no-sign          Do not sign apk.


λ java -jar dpt.jar -f org_v01.apk 
//他会执行 ProxyAppComponentFactory实现相关加载,与此同时ProxyApplication不会得到执行

λ java -jar dpt.jar -c -f org_v01.apk 
//他会执行 ProxyApplication实现相关加载,与此同时ProxyAppComponentFactory不会得到执行

ps: 至于为什么,我也不知道,QAQ
```





## 二代| gen2_v1

基于 [dpt-shell](https://github.com/luoyesiqiu/dpt-shell) 的项目

在该项目上做了注释, 可能也做了一丢丢修改



该项目也可以理解为在一代壳的基础上, 多了函数抽取的功能, 涉及到 一些函数的hook





