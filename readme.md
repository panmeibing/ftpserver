#### 说明

该APP作用是建立FTP服务，方便传输文件

基于Apache开源项目FtpServer（http://mina.apache.org/ftpserver-project）

#### 已知问题

使用AndroidStudio的build.gradle获取的官方jar包**不能很好支持中文字符**，故构建APP之前可以考虑换成民间高手修改的jar包，已放在`app/libs`文件夹
AndroidStudio缓存的jar包默认位置是`C:\Users用户名\.gradle\caches\modules-2\files-2.1\org.apache.ftpserver\ftpserver-core\1.1.1\XXX`

目前替换jar包后在win10家庭版测试未发现中文文件名报错的问题，不知是否会引入其他BUG