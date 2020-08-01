package com.tsangle.sharesj.HttpServer;

import com.tsangle.sharesj.Handler.*;
import com.tsangle.sharesj.Handler.Local.LocalFileHandler;
import com.tsangle.sharesj.Handler.Local.LocalImageHandler;
import com.tsangle.sharesj.Handler.Local.LocalResourceHandler;
import com.tsangle.sharesj.Handler.Local.LocalVideoHandler;

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

    public HttpListener(){
        try{
            serverSocket=new ServerSocket(ServiceNode.GetInstance().GetPort());
            handlerDispenser =new HandlerDispenser();
            handlerDispenser.AddHandler(new ResourceHandler());
            handlerDispenser.AddHandler(new FileHandler());
            handlerDispenser.AddHandler(new ImageHandler());
            handlerDispenser.AddHandler(new VideoHandler());
            handlerDispenser.AddHandler(new LocalResourceHandler());
            handlerDispenser.AddHandler(new LocalFileHandler());
            handlerDispenser.AddHandler(new LocalImageHandler());
            handlerDispenser.AddHandler(new LocalVideoHandler());
        }catch (Exception e){
            StringWriter stringWriter = new StringWriter();
            e.printStackTrace(new PrintWriter(stringWriter));
            logger.warning(stringWriter.toString());
        }
    }

    class AcceptRequestRunnable implements Runnable{
        class HandleRequestRunnable implements Runnable{
            private Socket socket;

            HandleRequestRunnable(Socket socket){
                this.socket=socket;
            }

            @Override
            public void run() {
                try{
                    handlerDispenser.Handle(new RequestSocket(socket));
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
                    if(socket!=null){
                        Runnable runnable=new HandleRequestRunnable(socket);
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
