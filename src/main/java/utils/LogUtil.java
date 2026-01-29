package utils;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.notification.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.IdeFrame;
import com.intellij.openapi.wm.WindowManager;
import org.apache.commons.lang3.StringUtils;

import java.awt.*;
import java.util.ArrayList;

import javax.swing.*;

public class LogUtil {

	private static Project lasted_open_project;

    public static void setNotifyProject(Project project) {
		lasted_open_project = project;
    }

    public static void notifyInfo(String text) {
        boolean closeLog = PropertiesComponent.getInstance().getBoolean("key_close_log");
        if (!closeLog) {
			NotificationGroupManager.getInstance().getNotificationGroup("Leeks Notification Group")
					.createNotification("Leeks: " + text, NotificationType.INFORMATION)
					.notify(getProject());
        }
    }

	public static void notifyWarn(String text) {
		NotificationGroupManager.getInstance().getNotificationGroup("Leeks Notification Group")
				.createNotification("Leeks: " + text, NotificationType.WARNING)
				.notify(getProject());
	}

	public static void notifyError(String text) {
		NotificationGroupManager.getInstance().getNotificationGroup("Leeks Notification Group")
				.createNotification("Leeks: " + text, NotificationType.ERROR)
				.notify(getProject());
	}

	public static Project getProject() {
		Window window = WindowManager.getInstance().getMostRecentFocusedWindow();
		if (window instanceof IdeFrame) {
			return ((IdeFrame) window).getProject();
		}
		if (lasted_open_project != null) {
			return lasted_open_project;
		}
		return null;
	}
}
