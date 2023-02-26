package com.bg7yoz.ft8cn.flex;

public enum FlexResponseStyle {
    STATUS,//状态信息，S+HANDLE
    RESPONSE,//命令的响应，R+客户端命令序列号
    HANDLE,//电台给定的句柄，H+句柄（32位的16进制表示）
    VERSION,//版本号，V+版本号
    MESSAGE,//电台发送的消息，M+消息好|消息文本
    COMMAND,//发送命令，C+序列号|命令
    UNKNOW//未知的回复类型
}
