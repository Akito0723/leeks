package bean;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class YahooResponse {

    private Result quoteResponse;

	@Getter
	@Setter
    public static class Result {

        private List<CoinBean> result;

    }
}

