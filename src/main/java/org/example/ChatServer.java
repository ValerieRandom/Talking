package org.example;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;

import java.util.HashSet;
import java.util.Set;

public class ChatServer extends AbstractVerticle {
    private final Set<ServerWebSocket> connections = new HashSet<>();

    @Override
    public void start() {
        HttpServer server = vertx.createHttpServer();
        Router router = Router.router(vertx);

        // 提供靜態資源
        StaticHandler staticHandler = StaticHandler.create("."); // 從 JAR 根目錄讀取
        staticHandler.setAllowRootFileSystemAccess(false);
        staticHandler.setFilesReadOnly(true);
        staticHandler.setCachingEnabled(false); // 禁用緩存，方便測試
        router.get("/index.html").handler(ctx -> {
            System.out.println("收到 /index.html 請求");
            staticHandler.handle(ctx);
        });
        router.get("/").handler(ctx -> {
            System.out.println("收到根路徑請求，重定向到 /index.html");
            ctx.reroute("/index.html");
        });

        server.requestHandler(router);
        server.webSocketHandler(webSocket -> {
            connections.add(webSocket);
            System.out.println("新用戶連線，總數: " + connections.size());

            webSocket.textMessageHandler(message -> {
                System.out.println("收到訊息: " + message);
                for (ServerWebSocket ws : connections) {
                    ws.writeTextMessage(message);
                }
            });

            webSocket.closeHandler(v -> {
                connections.remove(webSocket);
                System.out.println("用戶斷線，總數: " + connections.size());
            });
        });

        server.listen(8080, res -> {
            if (res.succeeded()) {
                System.out.println("伺服器啟動於 http://localhost:8080");
                System.out.println("嘗試訪問: http://localhost:8080/index.html");
            } else {
                System.out.println("啟動失敗: " + res.cause());
            }
        });

        System.out.println("index.html 是否存在: " + vertx.fileSystem().existsBlocking("index.html"));
    }
}