package servent.message.util;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;

import app.AppConfig;
import app.ServentInfo;
import servent.message.Message;
import servent.message.SnapshotIndicator;

/**
 * This worker sends a message asynchronously. Doing this in a separate thread
 * has the added benefit of being able to delay without blocking main or somesuch.
 * 
 * @author bmilojkovic
 *
 */
public class DelayedMessageSender implements Runnable {

	private Message messageToSend;
	
	public DelayedMessageSender(Message messageToSend) {
		this.messageToSend = messageToSend;
	}
	
	public void run() {
		/*
		 * A random sleep before sending.
		 * It is important to take regular naps for health reasons.
		 */
		try {
			Thread.sleep((long)(Math.random() * 1000) + 500);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		
		ServentInfo receiverInfo = messageToSend.getReceiverInfo();
		
		
		
		try {
			/*
			 * Similar sync block to the one in FifoSenderWorker, except this one is
			 * related to Lai-Yang. We want to be sure that message color is red if we
			 * are red. Just setting the attribute when we were making the message may
			 * have been to early.
			 * All messages that declare their own stuff (eg. LYTellMessage) will have
			 * to override setRedColor() because of this.
			 */
			synchronized (AppConfig.colorLock) {
				
					//messageToSend = messageToSend.setRedColor();
					SnapshotIndicator snap =  new SnapshotIndicator(AppConfig.currentSnapshotInitiator, AppConfig.currentSnapshotId.get());
					messageToSend.setSnapshotIndicator(snap);

					if (MessageUtil.MESSAGE_UTIL_PRINTING) {
						AppConfig.timestampedStandardPrint("Sending message " + messageToSend);
					}
				Socket sendSocket = new Socket(receiverInfo.getIpAddress(), receiverInfo.getListenerPort());
				
				ObjectOutputStream oos = new ObjectOutputStream(sendSocket.getOutputStream());
				oos.writeObject(messageToSend);
				oos.flush();
				
				sendSocket.close();
				AppConfig.timestampedErrorPrint("//////msgID:" + messageToSend.getMessageId());
				messageToSend.sendEffect();
			}
			
		} catch (IOException e) {
			AppConfig.timestampedErrorPrint("Couldn't send message: " + messageToSend.toString());
		}
	}
	
}
