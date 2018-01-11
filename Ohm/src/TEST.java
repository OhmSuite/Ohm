import java.util.Optional;
import java.util.function.Consumer;

public class TEST {
	public void go() {
		Optional<String> s = Optional.empty();
		String st = s.orElse("hi");
		s.ifPresent(str -> str.trim());
		char c = s.map(str -> str.charAt(0)).orElse('c');
		Consumer<String> config = stt -> {

		};
	}

	public void doThis(String theString) {
		System.out.println(theString);
	}

	public class StringWrapper {
		private Optional<String> string;

		public StringWrapper(Consumer<String> config) {
			string = Optional.empty();
			string.ifPresent(config);
		}

		public String getFirstThree() {
			return string.map(s -> s.substring(0, 3)).orElse("XXX");
		}
	}
}
