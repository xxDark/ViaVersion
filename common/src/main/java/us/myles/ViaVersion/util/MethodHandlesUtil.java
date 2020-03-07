package us.myles.ViaVersion.util;

import lombok.experimental.UtilityClass;
import lombok.val;

import java.lang.invoke.MethodHandles;

@UtilityClass
public class MethodHandlesUtil {
	private final MethodHandles.Lookup LOOKUP;

	public MethodHandles.Lookup lookup() {
		return LOOKUP;
	}

	static {
		try {
			MethodHandles.publicLookup();
			val field = MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP");
			field.setAccessible(true);
			LOOKUP = (MethodHandles.Lookup) field.get(null);
		} catch (IllegalAccessException | NoSuchFieldException ex) {
			// thanks, java.
			val err = new ExceptionInInitializerError("Failed to locate IMPL_LOOKUP, please file an issue.");
			err.initCause(ex);
			throw err;
		}
	}
}
