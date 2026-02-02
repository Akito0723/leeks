package bean;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 *
 * @author akihiro
 * @date 2026/02/02.
 */
@Getter
@Setter
public class EastMoneyResponse {

	private String rc;

	private String rt;

	private String svr;

	private String lt;

	private String full;

	private String dlmkts;

	private Data data;

	@Getter
	@Setter
	public static class Data {

		private int total;
		private List<Diff> diff;

		@Getter
		@Setter
		public static class Diff {
			private String f1;
			private String f2;
			private String f3;
			private String f4;
			private String f5;
			private String f6;
			private String f7;
			private String f8;
			private String f9;
			private String f10;
			private String f12;
			private String f13;
			private String f14;
			private String f15;
			private String f16;
			private String f18;
			private String f19;
			private String f22;
			private String f30;
			private String f31;
			private String f32;
			private String f88;
			private String f100;
			private String f112;
			private String f124;
			private String f125;
			private String f139;
			private String f148;
			private String f152;
			private String f153;
		}
	}
}


