package utils;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.components.JBTabbedPane;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.net.URI;

/**
 * intellij ui 弹窗展示工具类 <br>
 * <a href="https://plugins.jetbrains.com/docs/intellij/popups.html#popups">...</a>
 */
public class PopupsUiUtil {

	private static final String loading_text = "加载中...";

	private static final Object BALLOON_LOCK = new Object();
	private static final JPanel BALLOON_CONTENT = new JPanel(new BorderLayout());
	private static Balloon balloonInstance;

	private static Balloon getBalloonInstance() {
		synchronized (BALLOON_LOCK) {
			if (balloonInstance == null || balloonInstance.isDisposed()) {
				balloonInstance = JBPopupFactory.getInstance()
						.createBalloonBuilder(BALLOON_CONTENT)
						.setBorderInsets(new Insets(0, 0, 0, 0))
						.setHideOnClickOutside(true)
						.createBalloon();
			}
			return balloonInstance;
		}
	}

	private static Balloon prepareBalloonContent(JComponent content) {
		Balloon balloon = getBalloonInstance();
		if (!balloon.isDisposed()) {
			balloon.hide();
		}
		BALLOON_CONTENT.removeAll();
		BALLOON_CONTENT.add(content, BorderLayout.CENTER);
		BALLOON_CONTENT.revalidate();
		BALLOON_CONTENT.repaint();
		return balloon;
	}
	/**
     * 弹窗展示图片
     *
     * @param fundCode    基金编码
     * @param showByPoint 窗口显示位置
     */
    public static void showImageByFundCode(String fundCode, FundShowType type, Point showByPoint) {
        //------试图解决个BUG，项目销毁的问题-------
        Project project = LogUtil.getProject();
        if (project == null || project.isDisposed()) {
            return;
        }

		// 使用占位Label（先不加载图片）
		JLabel imageLabel = new JLabel(loading_text, SwingConstants.CENTER);

		JBTabbedPane tabs = new JBTabbedPane();
		tabs.addTab(type.getDesc(), imageLabel);

		Balloon balloon = prepareBalloonContent(tabs);

		balloon.show(RelativePoint.fromScreen(showByPoint), Balloon.Position.atRight);

		// 图片接口
		// 带水印  http://j4.dfcfw.com/charts/pic6/590008.png
		// 无水印  http://j4.dfcfw.com/charts/pic7/590008.png
		// 异步加载图片 避免卡顿UI
		loadImageAsync(
				imageLabel, tabs, balloon,
				String.format("http://j4.dfcfw.com/charts/pic7/%s.png?%s",
						fundCode, System.currentTimeMillis()),
				project);
    }

    /**
     * 弹窗展示图片
     *
     * @param stockCode   编码
     * @param selectType  展示的类型
     * @param showByPoint 窗口显示位置
     */
    public static void showImageByStockCode(String stockCode, StockShowType selectType, Point showByPoint) {
		Project project = LogUtil.getProject();
		if (project == null || project.isDisposed()) {
			return;
		}

		// 用 JBTabbedPane 替代 JBTabsImpl
		JBTabbedPane tabs = new JBTabbedPane();

		// 创建 Balloon 弹窗
		Balloon balloon = prepareBalloonContent(tabs);

		for (StockShowType type : StockShowType.values()) {
			String imageUrlByStock = getImageUrlByStock(stockCode, type);

			// 占位 Label
			JLabel imageLabel = new JLabel(loading_text, SwingConstants.CENTER);

			tabs.addTab(type.getDesc(), imageLabel);

			// 如果是默认选中 tab，先加载图片
			if (type.equals(selectType)) {
				tabs.setSelectedComponent(imageLabel);
				loadImageAsync(imageLabel, tabs, balloon, imageUrlByStock, project);
			}
		}

		// tab 切换时加载图片
		tabs.addChangeListener(e -> {
			Component selected = tabs.getSelectedComponent();
			if (selected instanceof JLabel label) {
				if (label.getIcon() == null &&
						StringUtils.isNotBlank(label.getText()) &&
						!StringUtils.equals(label.getText(), loading_text)) {
					loadImageAsync(label, tabs, balloon, label.getText(), project);
				}
			}
		});

		balloon.show(RelativePoint.fromScreen(showByPoint), Balloon.Position.atRight);
    }

	/**
	 * 获取图片链接
	 *
	 * @param stockCode 股票编码
	 * @param type      枚举类型
	 * @return 可能为 null
	 */
	private static String getImageUrlByStock(String stockCode, StockShowType type) {
		String prefix = StringUtils.substring(stockCode, 0, 2);
		String url = "http://image.sinajs.cn/newchart/";
		switch (prefix) {
			case "sh":
			case "sz":
				// 沪深股
				// 分时线图  http://image.sinajs.cn/newchart/min/n/sh600519.gif
				// 日K线图  http://image.sinajs.cn/newchart/daily/n/sh600519.gif
				// 周K线图  http://image.sinajs.cn/newchart/weekly/n/sh600519.gif
				// 月K线图  http://image.sinajs.cn/newchart/monthly/n/sh600519.gif
				url = String.format("%s/%s/n/%s.gif?%s", url, type.getType(), stockCode, System.currentTimeMillis());
				break;
			case "us":
				// 美股
				// 分时线图 http://image.sinajs.cn/newchart/png/min/us/AAPL.png
				// 日K线图 http://image.sinajs.cn/newchart/usstock/daily/aapl.gif
				// 周K线图 http://image.sinajs.cn/newchart/usstock/weekly/aapl.gif
				// 月K线图 http://image.sinajs.cn/newchart/usstock/monthly/aapl.gif
				if (StockShowType.min.equals(type)) {
					url = String.format("%s/png/%s/%s/%s.png?%s", url, type.getType(), prefix, StringUtils.substring(stockCode, 2),
							System.currentTimeMillis());
				} else {
					url = String.format("%s/%sstock/%s/%s.gif?%s", url, prefix, type.getType(), StringUtils.substring(stockCode, 2),
							System.currentTimeMillis());
				}
				break;
			case "hk":
				// 港股
				// 分时线图 http://image.sinajs.cn/newchart/png/min/hk/02202.png
				// 日K线图 http://image.sinajs.cn/newchart/hk_stock/daily/02202.gif
				// 周K线图 http://image.sinajs.cn/newchart/hk_stock/weekly/02202.gif
				// 月K线图 http://image.sinajs.cn/newchart/hk_stock/monthly/02202.gif
				if (StockShowType.min.equals(type)) {
					url = String.format("%s/png/%s/%s/%s.png?%s", url, type.getType(), prefix, StringUtils.substring(stockCode, 2),
							System.currentTimeMillis());
				} else {
					url = String.format("%s/%s_stock/%s/%s.gif?%s", url, prefix, type.getType(), StringUtils.substring(stockCode, 2),
							System.currentTimeMillis());
				}
				break;
			default:
				return "";
		}
		return url;
	}

	/**
	 * 异步加载图片
	 */
	private static void loadImageAsync(JLabel label,
			JBTabbedPane tabs,
			Balloon balloon,
			String imageUrl, Project project) {
		ApplicationManager.getApplication().executeOnPooledThread(() -> {
			try {
				URI uri = new URI(imageUrl);
				ImageIcon icon = new ImageIcon(uri.toURL());
				Image img = icon.getImage();
				int w = img.getWidth(null);
				int h = img.getHeight(null);
				SwingUtilities.invokeLater(() -> {
					if (!project.isDisposed()) {
						label.setText(null);
						label.setIcon(icon);
						// 设置 JLabel 尺寸等于图片原始大小
						label.setPreferredSize(new Dimension(w, h));
						label.setSize(new Dimension(w, h));
						// 刷新大小
						label.revalidate();
						label.repaint();
						tabs.revalidate();
						tabs.repaint();
						balloon.revalidate();
					}
				});
			} catch (Exception e) {
				SwingUtilities.invokeLater(() -> label.setText("图片加载失败"));
			}
		});
	}

	public interface BaseShowType {
		String getDesc();
	}

	@Getter
    public enum FundShowType implements BaseShowType {
        /**
         * 净值估算图
         */
        gsz("净值估算图");
        private final String desc;

        FundShowType(String desc) {
            this.desc = desc;
        }

	}

    @Getter
	public enum StockShowType  implements BaseShowType{
        /**
         * 分时线图
         */
        min("min", "分时线图"),
        /**
         * 日K 线图
         */
        daily("daily", "日K线图"),
        /**
         * 周K 线图
         */
        weekly("weekly", "周K线图"),
        /**
         * 月K 线图
         */
        monthly("monthly", "月K线图");

        private final String type;
        private final String desc;

        StockShowType(String type, String desc) {
            this.type = type;
            this.desc = desc;
        }

	}
}
