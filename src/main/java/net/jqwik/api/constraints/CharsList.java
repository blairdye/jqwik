package net.jqwik.api.constraints;

import java.lang.annotation.*;

@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CharsList {
	Chars[] value();
}
