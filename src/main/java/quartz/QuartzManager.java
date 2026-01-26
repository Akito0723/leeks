package quartz;

import java.text.ParseException;
import java.util.Map;
import java.util.Properties;
import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import utils.LogUtil;

/**
 * 任务管理，请参考文档 <a href="http://www.quartz-scheduler.org/documentation">...</a>
 *
 * @author dengerYang
 * @date 2021年12月27日
 */
public class QuartzManager {

	private static final Logger log = Logger.getInstance(QuartzManager.class);


	private Scheduler sched = null;
    private String instanceName;

    private QuartzManager() {
    }

    private QuartzManager(Scheduler sched, String instanceName) {
        this.sched = sched;
        this.instanceName = instanceName;
    }

    /**
     * 初始化
     *
     * @param instanceName 实例名称，根据此名称识别任务资源。
     */
    public static QuartzManager getInstance(String instanceName) {
        try {
			// 线程能满足资源即可
            return getInstance(instanceName, 1);
        } catch (SchedulerException e) {
			log.error("quartzManager instanceName:" + instanceName + " 初始化失败,原因:", e);
            throw new RuntimeException("QuartzManager "+ instanceName + "初始化失败", e);
        }
    }

    public static QuartzManager getInstance(String instanceName, int threadCount) throws SchedulerException {
        Properties props = new Properties();
        props.put("org.quartz.scheduler.instanceName", instanceName);
        props.put("org.quartz.threadPool.threadCount", threadCount + "");
        StdSchedulerFactory stdSchedulerFactory = new StdSchedulerFactory(props);
        return new QuartzManager(stdSchedulerFactory.getScheduler(), instanceName);
    }

    /**
     * 校验 Cron 表达式
     * @param cronExpression 表达式
     */
    public static boolean checkCronExpression(String cronExpression) {
        try {
            new CronExpression(cronExpression);
            return true;
        } catch (ParseException e) {
			log.warn("校验 Cron 表达式 ["+ cronExpression + "] 时出现异常,原因:", e);
            return false;
        }
    }

    /**
     * 添加一个主定时任务，如果存在会替换。注意，只有运行一个任务，每次执行都会清除掉之前任务
     *
     * @param clazz          jobCLass
     * @param cronExpression cron表达式，支持;分隔
     * @param dataMap        传递给 job 的参数
     */
    public void runJob(Class<? extends Job> clazz, @NotNull String cronExpression, Map<? extends String, ?> dataMap) {
        try {
            sched.clear();
            String[] split = cronExpression.split(";");
            for (int i = 0; i < split.length; i++) {
                String cron = split[i];
                JobDetail detail = JobBuilder.newJob(clazz).withIdentity(instanceName + i).build();
                if (dataMap != null && !dataMap.isEmpty()) {
                    detail.getJobDataMap().putAll(dataMap);
                }
                LogUtil.info("Leeks 创建定时任务 [ " + cron + " ] " + dataMap.get(HandlerJob.KEY_HANDLER).getClass().getSimpleName());
                sched.scheduleJob(detail, TriggerBuilder.newTrigger().withSchedule(CronScheduleBuilder.cronSchedule(cron)).build());
            }
            // 启动
            if (!sched.isShutdown()) {
                sched.start();
            }
        } catch (SchedulerException e) {
			log.error("quartzManager instanceName:" + this.instanceName + "执行定时任务失败,原因:", e);
			throw new RuntimeException("QuartzManager "+ instanceName + "执行定时任务失败", e);
        }
    }

    public void stopJob() {
        try {
			// 清除资源
            sched.clear();
            sched.shutdown();
        } catch (SchedulerException e) {
			log.error("quartzManager instanceName:" + this.instanceName + "停止定时任务失败,原因:", e);
			throw new RuntimeException("QuartzManager "+ instanceName + "停止定时任务失败", e);
        }
    }
}
