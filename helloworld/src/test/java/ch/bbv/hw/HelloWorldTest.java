package ch.bbv.hw;

import java.io.PrintStream;

import org.junit.Test;
import org.mockito.Mockito;

public class HelloWorldTest {

	@Test
	public void printMessageShowsHelloWorld() {
		PrintStream out = System.out;

		try {
			PrintStream spyOut = Mockito.spy(out);
			System.setOut(spyOut);
			HelloWorld.main(null);
			Mockito.verify(spyOut).println("Hello World");
		} finally {
			System.setOut(out);
		}
	}
}
