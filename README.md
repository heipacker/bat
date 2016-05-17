# agentTest
java agent test project
就是一个java agent的例子， 包括启动attach, 启动后attach;
只能在linux上运行， 因为没有WindowsVirtualMachine.class,BsdVirtualMachine.class,SolarisVirtualMachine.class
如果需要支持这个， 可以到响应的jdk版本中把tools.jar里面的这个拷贝过来
具体运行方法， 参考main函数的注释

使用byte dubby 修改字节码， 添加执行过程拦截记录

这里准备写一个跟踪链系统, 参考dapper
先把字节码转换的地方搞好。
