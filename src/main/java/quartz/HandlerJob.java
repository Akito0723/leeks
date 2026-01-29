package quartz;

import java.util.List;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.ExceptionUtil;
import handler.BaseTableRefreshHandler;
import org.quartz.*;
import utils.LogUtil;

/**
 * 执行时钟任务，不想定义那么多类了，所以写了一个共用的，后面有特殊在各个单独处理。<br/>
 * 使用时请注意，将KEY_HANDLER和KEY_CODES，放入参数中
 * <pre>
 *                 QuartzManager quartzManager = new QuartzManager("实例名称");
 *                 HashMap<String, Object> dataMap = new HashMap<>();
 *                 dataMap.put(HandlerJob.KEY_HANDLER, handler);
 *                 dataMap.put(HandlerJob.KEY_CODES, codes);
 *                 quartzManager.runJob(HandlerJob.class, instance.getValue("key_cron_expression_stock"), dataMap); // 添加任务并执行
 * </pre>
 * 请参考文档 <a href="http://www.quartz-scheduler.org/documentation">...</a>
 *
 * @author dengerYang
 * @date 2021年12月27日
 */
@DisallowConcurrentExecution
public class HandlerJob implements Job {

	public static final String KEY_HANDLER = "handler";

	public static final String KEY_CODES = "codes";

	private static final Logger log = Logger.getInstance(HandlerJob.class);

    @Override
	@SuppressWarnings("unchecked")
    public void execute(JobExecutionContext context) throws JobExecutionException {
		JobDataMap dataMap = context.getMergedJobDataMap();
        try {
            Object handler = dataMap.get(KEY_HANDLER);
			List<String> configCodeList = (List<String>) dataMap.get(KEY_CODES);

            if (handler instanceof BaseTableRefreshHandler) {
                ((BaseTableRefreshHandler) handler).refreshTableUIData(configCodeList);
            }

        } catch (Exception e) {
			log.error("刷新出现异常,原因:", e);
            LogUtil.notifyInfo("刷新出现异常：" + ExceptionUtil.getMessage(e) + "\r\n" + ExceptionUtil.currentStackTrace());
			throw new JobExecutionException(e);
        }
    }
}
