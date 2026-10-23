package techguns.util;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;

/** Read-only stub of the 1.12 class: the block's registry name follows the fields as UTF. */
public class MBlock implements Serializable {
	private static final long serialVersionUID = 1L;
	public int meta;
	public transient String name;

	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.defaultReadObject();
		name = in.readUTF();
	}
}
