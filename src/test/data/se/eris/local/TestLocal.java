package se.eris.local;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TestLocal {

	public void method(){
		class LocalClass {

			public LocalClass(@Nullable String nullable, @NotNull String notNull) {

			}

			public void localMethod(@Nullable String nullable, @NotNull String notNull) {

			}
		}
	}
}
