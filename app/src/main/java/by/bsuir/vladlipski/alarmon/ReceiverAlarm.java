package by.bsuir.vladlipski.alarmon;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;


public class ReceiverAlarm extends BroadcastReceiver {
  @Override
  public void onReceive(Context context, Intent recvIntent) {
    Uri alarmUri = recvIntent.getData();
    long alarmId = AlarmUtil.alarmUriToId(alarmUri);

    try {
      WakeLock.acquire(context, alarmId);
    } catch (WakeLock.WakeLockException e) {
    }

    Intent notifyService = new Intent(context, NotificationService.class);
    notifyService.setData(alarmUri);

    context.startService(notifyService);
  }
}
