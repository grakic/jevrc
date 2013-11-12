package net.devbase.jevrc.tlv;

import java.util.Map;

public abstract class Tlv {

	protected byte[] tag;
	protected int length;

	public byte[] getTag() {
		return tag;
	}

	public int getLength() {
		return length;
	}

	@Override
	public String toString() {
		return toString(0);
	}

	abstract protected String toStringContent(int level);
	
	public String toString(int level) {
		String indent = new String(new char[level * 2]).replace("\0", " ");
		return indent + toStringContent(level);
	}

	protected String getTagKeyWithPrefix(String tagPrefix) {
		return (tagPrefix == null ? "" : tagPrefix + ".") + Utils.bytes2HexStringCompact(tag);
	}
	
	abstract void addValuesToDict(String tagPrefix, Map<String, byte[]> dict);	
}
