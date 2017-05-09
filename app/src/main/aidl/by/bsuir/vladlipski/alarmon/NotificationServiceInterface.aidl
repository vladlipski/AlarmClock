package by.bsuir.vladlipski.alarmon;

interface NotificationServiceInterface {
  long currentAlarmId();
  int firingAlarmCount();
  void acknowledgeCurrentNotification(int snoozeMinutes);
}