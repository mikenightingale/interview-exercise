package com.acme.mytrader.strategy;
import com.acme.mytrader.execution.ExecutionService;
import com.acme.mytrader.price.PriceSource;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class TradingStrategyTest {
	@Mock
	private ExecutionService executionService;
	@Mock
	private PriceSource priceSource;
	static final double THRESHOLD= 100d;
	static final double BELOW_THRESHOLD= 50d;
	static final double ABOVE_THRESHOLD= 110d;
	private TradingStrategy subject;
	public static final int LOTS = 5000;

	@Before
	public void setUp()  {
		subject = new TradingStrategy("IBM", THRESHOLD, LOTS, executionService, priceSource);
		subject.connect();
	}

	@Test
	public void shouldRegisterListenerOnConnect()  {
		verify(priceSource, times(1)).addPriceListener(subject);
	}

	@Test
	public void shouldRegisterListenerOnce()  {
		subject.connect();
		verify(priceSource, times(1)).addPriceListener(subject);
	}

	@Test
	public void shouldRemoveListener()  {
		subject.disconnect();
		verify(priceSource, times(1)).removePriceListener(subject);
	}

	@Test
	public void shouldExecuteBuyOrder()  {
		whenPriceUpdateIsPublished(BELOW_THRESHOLD);
		verify(executionService, times(1)).buy(eq("ibm"), eq(BELOW_THRESHOLD), eq(LOTS));
	}

	@Test
	public void shouldExecuteBuyOrderOnceOnly()  {
		whenPriceUpdateIsPublished(BELOW_THRESHOLD);
		verify(executionService, times(1)).buy(eq("ibm"), eq(BELOW_THRESHOLD), eq(LOTS));
		whenPriceUpdateIsPublished(BELOW_THRESHOLD);
		verifyNoMoreInteractions(executionService);
	}

	@Test
	public void shouldExecuteEachBuyOrderOnceOnly()  {
		TradingStrategy another = new TradingStrategy("IBM", 45d, LOTS, executionService, priceSource);
		another.connect();

		whenPriceUpdateIsPublished(BELOW_THRESHOLD);
		verify(executionService).buy(eq("ibm"), eq(BELOW_THRESHOLD), eq(LOTS));

		another.priceUpdate("ibm", 40d);
		verify(executionService).buy(eq("ibm"), eq(40d), eq(LOTS));
	}

	@Test
	public void shouldIgnorePriceTickAtThreshold() {
		whenPriceUpdateIsPublished(THRESHOLD);
		verifyZeroInteractions(executionService);
	}

	@Test
	public void shouldIgnorePriceTickAboveThreshold() {
		whenPriceUpdateIsPublished(ABOVE_THRESHOLD);
		verifyZeroInteractions(executionService);
	}

	@Test
	public void shouldNotActionPriceTicksForOtherSecurities() {
		subject.priceUpdate("DBK", 100d);
		verifyZeroInteractions(executionService);
	}

	@Test
	public void shouldNotReceivePriceTicksAfterDisconnect()  {
		subject.disconnect();
		whenPriceUpdateIsPublished(BELOW_THRESHOLD);
		verifyZeroInteractions(executionService);
	}

	// Fake the price source emitting an a price change
	private void whenPriceUpdateIsPublished(double price) {
		subject.priceUpdate("IBM", price);
	}

}
