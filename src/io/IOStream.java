package io;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;
// 封装好的IO接口
public class IOStream {

	public static Object readMessage(Socket socket) {
		Object obj = null;
		try {
			InputStream is = socket.getInputStream();
			ObjectInputStream ois = new ObjectInputStream(is);
			obj = ois.readObject();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return obj;
	}
	

	public static boolean writeMessage(Socket socket, Object message) {
		try {
			OutputStream os = socket.getOutputStream();
			ObjectOutputStream oos = new ObjectOutputStream(os);
			oos.writeObject(message);
			oos.flush();//刷新以发送消息
			return true;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}
}
