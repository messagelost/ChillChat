package com.javaclimb.chillchat.entity.config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


@Component("appConfig")
public class AppConfig {
    /**
     * websocket端口
     */
    @Value("${ws.port:}")
    private Integer wsPort;
    @Value("${projext.folder}")
    private String projectFolder;
    @Value("${admin.emails}")
    private String adminEmail;

    public Integer getWsPort() {
        return wsPort;
    }

    public String getProjectFolder() {
        return projectFolder;
    }

    public String getAdminEmail() {
        return adminEmail;
    }
}
