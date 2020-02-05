package com.tsangle.sharesj.HttpServer;

import com.tsangle.sharesj.Handler.HandlerDispenser;
import com.tsangle.sharesj.Handler.PathHandler;
import com.tsangle.sharesj.Handler.ResourceHandler;
import com.tsangle.sharesj.Model.RequestSocket;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.logging.Logger;

public class HttpListener {
    private static Logger logger = Logger.getLogger(HttpListener.class.getName());

    private Thread listenThread;

    private ServerSocket serverSocket;

    private HandlerDispenser handlerDispenser;

    public HttpListener(int port){
        try{
            serverSocket=new ServerSocket(port);
            handlerDispenser =new HandlerDispenser();
            handlerDispenser.AddHandler(new ResourceHandler());
            handlerDispenser.AddHandler(new PathHandler());
        }catch (Exception e){
            StringWriter stringWriter = new StringWriter();
            e.printStackTrace(new PrintWriter(stringWriter));
            logger.warning(stringWriter.toString());
        }
    }

    class AcceptRequestRunnable implements Runnable{
        class HandleRequestRunnable implements Runnable{
            private RequestSocket requestSocket;

            public HandleRequestRunnable(RequestSocket requestSocket){
                this.requestSocket=requestSocket;
            }

            @Override
            public void run() {
                try{
                    handlerDispenser.Handle(requestSocket);
                }catch (Exception e){
                    StringWriter stringWriter = new StringWriter();
                    e.printStackTrace(new PrintWriter(stringWriter));
                    logger.warning(stringWriter.toString());
                }
            }
        }

        @Override
        public void run() {
            try{
                while (true){
                    Socket socket=serverSocket.accept();
                    RequestSocket requestSocket=new RequestSocket(socket);
                    if(requestSocket.GetHost()!=null&&!requestSocket.GetHost().isEmpty()){
                        Runnable runnable=new HandleRequestRunnable(requestSocket);
                        Thread thread=new Thread(runnable);
                        thread.start();
                    }
                }
            }catch (SocketException e){
                logger.info("Server socket closed.");
            }catch (Exception e){
                StringWriter stringWriter = new StringWriter();
                e.printStackTrace(new PrintWriter(stringWriter));
                logger.warning(stringWriter.toString());
            }
        }
    }

    public void Start(){
        Runnable runnable=new AcceptRequestRunnable();
        listenThread=new Thread(runnable);
        listenThread.start();
    }

    public void Stop(){
        try{
            serverSocket.close();
            listenThread.join();
        }catch (Exception e){
            StringWriter stringWriter = new StringWriter();
            e.printStackTrace(new PrintWriter(stringWriter));
            logger.warning(stringWriter.toString());
        }
    }
}
