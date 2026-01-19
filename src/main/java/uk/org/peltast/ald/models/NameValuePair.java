package uk.org.peltast.ald.models;

/** Handles name - value pairs.
*
* @author MA Wheadon
* @copyright MA Wheadon, 2026.
* @licence MIT License.
*/
public class NameValuePair {
	private final String mName;
	private final String mValue;

	public NameValuePair(String name, String value) {
		mName = name;
		mValue = value;
	}

	public String getName() {
		return(mName);
	}

	public String getValue() {
		return(mValue);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof NameValuePair) {
			NameValuePair pair = (NameValuePair)obj;
			if (pair.mName.equals(mName) && pair.mValue.equals(mValue)) {
				return(true);
			}
		}
		return(false);
	}

	@Override
	public int hashCode() {
		return super.hashCode();
	}
	@Override
	public String toString() {
		return(getValue());	// so that a combo box renders the value not the name
	}

}
