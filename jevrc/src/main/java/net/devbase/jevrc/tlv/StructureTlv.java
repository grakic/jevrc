package net.devbase.jevrc.tlv;

import java.util.List;
import java.util.Map;

public class StructureTlv extends Tlv {
	
	private List<Tlv> children = null;

	public StructureTlv(byte[] tag, int length, List<Tlv> children) {
		this.tag = tag;
		this.length = length;
		this.children = children;
	}

	@Override
	protected String toStringContent(int level) {
		StringBuffer sb = new StringBuffer();
		sb.append(String.format("[%s] = { (%d)\n",
				Utils.bytes2HexString(tag), length));
		for (Tlv child : children) {
			sb.append(child.toString(level + 1));
		}
		String indent = new String(new char[level * 2]).replace("\0", " ");
		sb.append(indent);
		sb.append("}\n");
		return sb.toString();
	}

	@Override
	void addValuesToDict(String tagPrefix, Map<String, byte[]> dict) {
		for (Tlv tlv : children) {
			tlv.addValuesToDict(getTagKeyWithPrefix(tagPrefix), dict);
		}
	}

}
