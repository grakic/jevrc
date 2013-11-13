package net.devbase.jevrc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.smartcardio.Card;
import javax.smartcardio.CardChannel;
import javax.smartcardio.CardException;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;

import net.devbase.jevrc.tlv.Tlv;
import net.devbase.jevrc.tlv.TlvListUtils;
import net.devbase.jevrc.tlv.Utils;

@SuppressWarnings("restriction") // Various javax.smartcardio.*
public class EvrcCard {

	private Card card;
	private CardChannel channel;
	
    public EvrcCard(final Card card)
            throws IllegalArgumentException, SecurityException, IllegalStateException {

        this.card = card;
        channel = card.getBasicChannel();
    }

    private static final byte[] EF_Registration_A = {(byte) 0xD0, (byte) 0x01};
    private static final byte[] EF_Registration_B = {(byte) 0xD0, (byte) 0x11};
    private static final byte[] EF_Registration_C = {(byte) 0xD0, (byte) 0x21};
    private static final byte[] EF_SerbianRegis_D = {(byte) 0xD0, (byte) 0x31};

    private static final int BLOCK_SIZE = 0x20;
    
	private byte[] mergeByteArrays(byte[] first, byte[] second) {
		byte[] result = Arrays.copyOf(first, first.length + second.length);
		System.arraycopy(second, 0, result, first.length, second.length);
		return result;
	}
    
    /**
     * Read EF contents, selecting by file path.
     * 
     * The file length is read at 4B offset from the file. The method is not thread safe. Exclusive
     * card access should be enabled before calling the method.
     * 
     * TODO: Refactor to be in line with ISO7816-4 and BER-TLV, removing "magic" headers
     */
    private byte[] readElementaryFile(byte[] name) throws CardException {

        selectFile(name);

        // Read first block of bytes from the EF
        byte[] header = readBinary(0, BLOCK_SIZE);

        // Get the inner tag length at offset 21
        if (header.length < 22) {
            throw new CardException("Read EF file failed: File header is missing");
        }

        int HEADER_LENGTH_OFFSET = 21;
        
		int lenByteSize = 1;
		if ((header[HEADER_LENGTH_OFFSET] & 0x80) == 0x80) {
			lenByteSize += (0x7F & header[HEADER_LENGTH_OFFSET]);
		}
		if (lenByteSize > 1) {
			lenByteSize--;
			HEADER_LENGTH_OFFSET++;
		}
		byte[] lenBytes = Arrays.copyOfRange(header, HEADER_LENGTH_OFFSET, HEADER_LENGTH_OFFSET + lenByteSize);		
		int length = Utils.bytes2IntLE(lenBytes);
		int dataLength = length+HEADER_LENGTH_OFFSET+lenByteSize;
		
        // Read binary into buffer
		return mergeByteArrays(header, readBinary(BLOCK_SIZE, dataLength-BLOCK_SIZE));
    }

    /** Reads the content of the selected file starting at offset, at most length bytes */
    private byte[] readBinary(int offset, int length) throws CardException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        while (length > 0) {
            int readSize = Math.min(length, BLOCK_SIZE);
            ResponseAPDU response = channel.transmit(
                new CommandAPDU(0x00, 0xB0, offset >> 8, offset & 0xFF, readSize));
            if (response.getSW() != 0x9000) {
                throw new CardException(
                    String.format("Read binary failed: offset=%d, length=%d, status=%s", 
                        offset, length, Utils.int2HexString(response.getSW())));
            }

            try {
                byte[] data = response.getData();
                out.write(data);
                offset += data.length;
                length -= data.length;
            } catch (IOException e) {
                throw new CardException("Read binary failed: Could not write byte stream");
            }
        }

        return out.toByteArray();
    }

    
    
    private void selectAid() throws CardException {

    	byte[] df1 = {
			(byte) 0xA0, (byte) 0x00, (byte) 0x00, (byte) 0x00, 
			(byte) 0x03, (byte) 0x00, (byte) 0x00, (byte) 0x00
    	};
    	
    	byte[] df2 = {
			(byte) 0xF3, (byte) 0x81, (byte) 0x00, (byte) 0x00,
			(byte) 0x02, (byte) 0x53, (byte) 0x45, (byte) 0x52,
			(byte) 0x56, (byte) 0x4C, (byte) 0x04, (byte) 0x02,
			(byte) 0x01
    	};
    	
    	byte[] aid = {
        	(byte) 0xA0, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        	(byte) 0x77, (byte) 0x01, (byte) 0x08, (byte) 0x00, 
        	(byte) 0x07, (byte) 0x00, (byte) 0x00, (byte) 0xFE, 
        	(byte) 0x00, (byte) 0x00, (byte) 0xAD, (byte) 0xF2
        };    	

    	channel.transmit(new CommandAPDU(0x00, 0xA4, 0x04, 0x00, df1));
        channel.transmit(new CommandAPDU(0x00, 0xA4, 0x04, 0x00, df2));

        ResponseAPDU response3 = channel.transmit(new CommandAPDU(0x00, 0xA4, 0x04, 0x0C, aid));
        if(response3.getSW() != 0x9000) {
            throw new CardException(
                String.format("Select AID with name=%s failed: status=%s\n", 
            		Utils.bytes2HexString(aid),
                    Utils.int2HexString(response3.getSW())));
        }
    }
    
    /** Selects the elementary file to read, based on the name passed in. */
    private void selectFile(byte[] name) throws CardException {
        ResponseAPDU response = channel.transmit(new CommandAPDU(0x00, 0xA4, 0x02, 0x04, name));
        if(response.getSW() != 0x9000) {
            throw new CardException(
                String.format("Select failed: name=%s, status=%s", 
                    Utils.bytes2HexString(name), Utils.int2HexString(response.getSW())));
        }
    }
    
    public EvrcInfo readEvrcInfo() throws CardException {
      try {
        card.beginExclusive();
    	selectAid();

    	byte[] data;
    	
    	data = readElementaryFile(EF_Registration_A);
		List<Tlv> tlvs_a = TlvListUtils.parse(data);

		data = readElementaryFile(EF_Registration_B);
		List<Tlv> tlvs_b = TlvListUtils.parse(data);

		data = readElementaryFile(EF_Registration_C);
		List<Tlv> tlvs_c = TlvListUtils.parse(data);

		data = readElementaryFile(EF_SerbianRegis_D);
		List<Tlv> tlvs_d = TlvListUtils.parse(data);

		Map<String, byte[]> dict = new HashMap<String, byte[]>();
		TlvListUtils.getValuesDict(tlvs_a, dict);
		TlvListUtils.getValuesDict(tlvs_b, dict);
		TlvListUtils.getValuesDict(tlvs_c, dict);
		TlvListUtils.getValuesDict(tlvs_d, dict);

		/*
		for (Map.Entry<String, byte[]> e : dict.entrySet()) {
			System.out.format("%s = %s\n",
					e.getKey(), Utils.bytes2HexStringCompact(e.getValue()));
		}
		System.exit(0);
		*/
		
		EvrcInfo evrcinfo = new EvrcInfo();
		evrcinfo.addDocumentData(dict);
		evrcinfo.addVehicleData(dict);
		evrcinfo.addPersonalData(dict);
		return evrcinfo;
         }
         finally {
             card.endExclusive();
         }
    }
    
    /** Disconnects, but doesn't reset the card. */
    public void disconnect() throws CardException {
        disconnect(false);
    }

    public void disconnect(boolean reset) throws CardException {
        card.disconnect(reset);
        card = null;
    }

    @Override
    protected void finalize() {
        try {
            if (card != null) {
                disconnect(false);
            }
        } catch (CardException error) {
        }
    }
    
}
