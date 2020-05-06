package com.acme.mytrader.strategy;

import com.acme.mytrader.execution.ExecutionService;
import com.acme.mytrader.price.PriceListener;
import com.acme.mytrader.price.PriceSource;

import java.util.Objects;

/**
 * <pre>
 * User Story: As a trader I want to be able to monitor stock prices such
 * that when they breach a trigger level orders can be executed automatically
 * </pre>
 */
public class TradingStrategy implements PriceListener {

	private final String targetSecurity;
	private final double thresholdPrice;
	private final int lots;
	private final ExecutionService executionService;
	private final PriceSource priceSource;
	private boolean isConnected = false;

	public TradingStrategy(String security, double triggerPrice, int lots,
	                       ExecutionService executionService, PriceSource priceSource) {
		this.targetSecurity = security.toLowerCase();
		this.thresholdPrice = triggerPrice;
		this.lots = lots;
		this.executionService = executionService;
		this.priceSource = priceSource;
	}

	public synchronized void connect()  {
		// use connect method to ensure listener fully constructed before exposing self reference
		if (!isConnected) {
			priceSource.addPriceListener(this);
			isConnected = true;
		}
	}

	public synchronized void disconnect() {
		isConnected = false;
		priceSource.removePriceListener(this);
	}

	@Override
	public synchronized void priceUpdate(String security, double price) {
		if (price < thresholdPrice && security.equalsIgnoreCase(targetSecurity) && isConnected) {
			disconnect();
			//TODO execute asynch to minimise blocking
			executionService.buy(targetSecurity, price, lots);
		}
	}

	// assumes single listener per security-price pair
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		TradingStrategy that = (TradingStrategy) o;
		return Double.compare(that.thresholdPrice, thresholdPrice) == 0 &&
				targetSecurity.equals(that.targetSecurity);
	}

	@Override
	public int hashCode() {
		return Objects.hash(targetSecurity, thresholdPrice);
	}
}
