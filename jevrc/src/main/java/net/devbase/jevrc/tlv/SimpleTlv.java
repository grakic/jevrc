package net.devbase.jevrc.tlv;

import java.util.Map;

public class SimpleTlv extends Tlv {

	private byte[] value = null;

	/**
	 * 
	 * @param tag
	 *            Tag byte array
	 * @param length
	 *            Number of value bits
	 * @param value
	 *            Values byte array
	 */
	public SimpleTlv(byte[] tag, int length, byte[] value) {
		this.tag = tag;
		this.length = length;
		this.value = value;
	}
	
	@Override
	protected String toStringContent(int level) {
		return String.format("[%s] = (%d) <%s>\n",
				Utils.bytes2HexString(tag), length,
				Utils.bytes2HexString(value));
	}
	
	@Override
	void addValuesToDict(String tagPrefix, Map<String, byte[]> dict) {
		dict.put(getTagKeyWithPrefix(tagPrefix), value);
	}
}
