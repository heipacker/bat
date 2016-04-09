# agentTest
java agent test project
就是一个java agent的例子， 包括启动attach, 启动后attach;
只能在linux上运行， 因为没有WindowsVirtualMachine.class,BsdVirtualMachine.class,SolarisVirtualMachine.class
如果需要支持这个， 可以到响应的jdk版本中把tools.jar里面的这个拷贝过来
具体运行方法， 参考main函数的注释
