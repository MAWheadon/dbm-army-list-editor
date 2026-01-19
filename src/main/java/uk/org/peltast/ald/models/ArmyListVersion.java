package uk.org.peltast.ald.models;

/** Encapsulates an army list version.
*
* @author MA Wheadon
* @copyright MA Wheadon, 2026.
* @licence MIT License.
*/
public class ArmyListVersion implements Comparable<ArmyListVersion> {
	private final int mMajor;
	private final int mMinor;

	public ArmyListVersion(int major, int minor) {
		mMajor = major;
		mMinor = minor;
	}

	public int getMajorVerison() {return(mMajor);}
	public int getMinorVerison() {return(mMinor);}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof ArmyListVersion) {
			ArmyListVersion ver = (ArmyListVersion)obj;
			return(mMajor == ver.mMajor && mMinor == ver.mMinor);
		}
		return(false);
	}

	@Override
	public int hashCode() {
		return(mMajor * 1000 + mMinor);
	}

	@Override
	public String toString() {
		return(mMajor + "." + mMinor);
	}

	@Override
	public int compareTo(ArmyListVersion ver) {
		if (mMajor < ver.mMajor) {
			return(-1);
		}
		if (mMajor > ver.mMajor) {
			return(1);
		}
		if (mMinor < ver.mMinor) {
			return(-1);
		}
		if (mMinor > ver.mMinor) {
			return(1);
		}
		return 0;
	}
}
