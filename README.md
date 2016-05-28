# bat(蝙蝠)

最开始的时候是为了学习用agent运行是修改字节

java agent test project
就是一个java agent的例子， 包括启动attach, 启动后attach;
只能在linux上运行， 因为没有WindowsVirtualMachine.class,BsdVirtualMachine.class,SolarisVirtualMachine.class
如果需要支持这个， 可以到响应的jdk版本中把tools.jar里面的这个拷贝过来
具体运行方法， 参考main函数的注释


参考byte dubby的实现, 这个也agent也可以用在其他的系统上面了
使用byte dubby 修改字节码， 添加执行过程拦截记录

##note

#####2016/05/05
这里准备写一个跟踪链系统, 参考dapper

先把字节码转换的地方搞好。

#####2016/05/21
可以简单的插桩了

#####2016/05/27
trace初步可以了

trace的信息可以存在文件里, 也可以通过rpc方式发送到服务端


执行TestAgent中的main来测试

...