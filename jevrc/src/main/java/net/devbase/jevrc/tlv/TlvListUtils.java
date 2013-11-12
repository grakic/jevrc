package net.devbase.jevrc.tlv;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class TlvListUtils {
	
	private TlvListUtils() {
		
	}
	
	public static Map<String, byte[]> getValuesDict(List<Tlv> tlvs) {
		Map<String, byte[]> dict = new HashMap<String, byte[]>();		
		return getValuesDict(tlvs, dict);
	}

	public static Map<String, byte[]> getValuesDict(List<Tlv> tlvs, Map<String, byte[]> dict) {
		for (Tlv tlv : tlvs) {
			tlv.addValuesToDict(null, dict);
		}		
		return dict;
	}
	
	public static List<Tlv> parse(byte[] data) {
		return parse(data, 0, data.length);
	}
	
	public static List<Tlv> parse(byte[] data, int bytesFrom, int bytesTo) {

		List<Tlv> tlvs = new LinkedList<Tlv>();

		if (bytesFrom < 0 || bytesTo > data.length) {
			throw new RuntimeException("Index out of data range");
		}

		while (bytesFrom < bytesTo) {
			
			// get tag size
			int tagByteSize = 1;
			if ((data[bytesFrom] & 0x1F) == 0x1F) {
				tagByteSize++;
				while ((bytesFrom + tagByteSize - 1 < bytesTo)
						&& (data[bytesFrom + tagByteSize - 1] & 0x80) == 0x80) {
					tagByteSize++;
				}
			}

			// get tag
			byte[] tag = Arrays.copyOfRange(data, bytesFrom, bytesFrom + tagByteSize);
			bytesFrom += tagByteSize;

			// get length size
			int lenByteSize = 1;
			if ((data[bytesFrom] & 0x80) == 0x80) {
				lenByteSize += (0x7F & data[bytesFrom]);
			}
			if (lenByteSize > 1) {
				bytesFrom++;
				lenByteSize -= 1;
			}

			// get length
			byte[] lenBytes = Arrays.copyOfRange(data, bytesFrom, bytesFrom + lenByteSize);		
			int len = Utils.bytes2IntLE(lenBytes);
			bytesFrom += lenBytes.length;

			// get value
			Tlv tlv;
			if ((tag[0] & 0x20) == 0x20) {
				// new structure with subtags
				List<Tlv> childTlvs = parse(data, bytesFrom, bytesFrom + len);
				tlv = new StructureTlv(tag, len, childTlvs);
			} else {
				byte[] value = Arrays.copyOfRange(data, bytesFrom, bytesFrom + len);
				tlv = new SimpleTlv(tag, len, value);
			}
			
			bytesFrom += len;			
			tlvs.add(tlv);
		}

		return tlvs;
	}

}
