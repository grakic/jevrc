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

    private static final int BLOCK_SIZE = 0x64;

    private enum Country {
        SERBIA,
        EU
    }

    private Country currentCountry;
    
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

        int HEADER_SIZE = 0x20;
        
        // Read first block of bytes from the EF
        byte[] header = readBinary(0, HEADER_SIZE);

        // Get the second tag length
        if (header[0] != 0x78) {
            throw new CardException("Read EF file failed: File header is missing");
        }
        int offset = header[1] + 3;
        
        int lenByteSize = 1;
        if ((header[offset] & 0x80) == 0x80) {
            lenByteSize += (0x7F & header[offset]);
        }
        if (lenByteSize > 1) {
            lenByteSize--;
            offset++;
        }
        byte[] lenBytes = Arrays.copyOfRange(header, offset, offset + lenByteSize);        
        int length = Utils.bytes2IntLE(lenBytes);
        int dataLength = length+offset+lenByteSize;
        
        // Read binary into buffer
        return mergeByteArrays(header, readBinary(HEADER_SIZE, dataLength-HEADER_SIZE));
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

    	ResponseAPDU response;
    	
    	// Serbian eVRC AID, first guess
    	response = channel.transmit(new CommandAPDU(0x00, 0xA4, 0x04, 0x00, 
    			Utils.asByteArray(0xA0, 0x00, 0x00, 0x01, 0x51, 0x00, 0x00)));
    	if (response.getSW() == 0x9000) {
    		
    		channel.transmit(new CommandAPDU(0x00, 0xA4, 0x04, 0x00, 
				Utils.asByteArray(0xA0, 0x00, 0x00, 0x00, 0x77, 0x01, 0x08, 0x00, 0x07, 0x00, 0x00, 0xFE, 0x00, 0x00, 0x01, 0x00)));
    		
			response = channel.transmit(new CommandAPDU(0x00, 0xA4, 0x04, 0x0C,
				Utils.asByteArray(0xA0, 0x00, 0x00, 0x00, 0x77, 0x01, 0x08, 0x00, 0x07, 0x00, 0x00, 0xFE, 0x00, 0x00, 0xAD, 0xF2)));
	
        	if (response.getSW() == 0x9000) {
        	    specifyCurrentCountry(Country.SERBIA);
        	    return;
            }
    	}
    	
    	// Serbian eVRC AID, second guess
    	response = channel.transmit(new CommandAPDU(0x00, 0xA4, 0x04, 0x00, 
    			Utils.asByteArray(0xA0, 0x00, 0x00, 0x00, 0x03, 0x00, 0x00, 0x00)));
    	if (response.getSW() == 0x9000) {

    		channel.transmit(new CommandAPDU(0x00, 0xA4, 0x04, 0x00, 
				Utils.asByteArray(0xF3, 0x81, 0x00, 0x00, 0x02, 0x53, 0x45, 0x52, 0x56, 0x4C, 0x04, 0x02, 0x01)));

			response = channel.transmit(new CommandAPDU(0x00, 0xA4, 0x04, 0x0C,
					Utils.asByteArray(0xA0, 0x00, 0x00, 0x00, 0x77, 0x01, 0x08, 0x00, 0x07, 0x00, 0x00, 0xFE, 0x00, 0x00, 0xAD, 0xF2)));
			
        	if (response.getSW() == 0x9000) {
        	    specifyCurrentCountry(Country.SERBIA);
        	    return;
            }
    	}
    	
    	// Serbian eVRC AID, third guess
    	response = channel.transmit(new CommandAPDU(0x00, 0xA4, 0x04, 0x00, 
    			Utils.asByteArray(0xA0, 0x00, 0x00, 0x00, 0x18, 0x43, 0x4D, 0x00)));
    	if (response.getSW() == 0x9000) {

    		channel.transmit(new CommandAPDU(0x00, 0xA4, 0x04, 0x00, 
				Utils.asByteArray(0xA0, 0x00, 0x00, 0x00, 0x18, 0x34, 0x14, 0x01, 0x00, 0x65, 0x56, 0x4C, 0x2D, 0x30, 0x30, 0x31)));

			response = channel.transmit(new CommandAPDU(0x00, 0xA4, 0x04, 0x0C,
					Utils.asByteArray(0xA0, 0x00, 0x00, 0x00, 0x18, 0x65, 0x56, 0x4C, 0x2D, 0x30, 0x30, 0x31)));
			
        	if (response.getSW() == 0x9000) {
                specifyCurrentCountry(Country.SERBIA);
                return;
            }
    	}

    	// EU eVRC AID
    	response = channel.transmit(new CommandAPDU(0x00, 0xA4, 0x04, 0x0C, 
        		Utils.asByteArray(0xA0, 0x00, 0x00, 0x04, 0x56, 0x45, 0x56, 0x52, 0x2D, 0x30, 0x31)));
		
    	if (response.getSW() == 0x9000) {
    	    specifyCurrentCountry(Country.EU);
            return;
        }

        throw new CardException(
            String.format("Select AID for card ATR=%s failed: status=%s\n", 
            		card.getATR().toString(),
            		Utils.int2HexString(response.getSW())));
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

    private void specifyCurrentCountry(Country currentCountry) {
        this.currentCountry = currentCountry;
    }

    public EvrcInfo readEvrcInfo() throws CardException {
        try {
            card.beginExclusive();
            selectAid();

            byte[] data;

            Map<String, byte[]> dict = new HashMap<String, byte[]>();

            data = readElementaryFile(EF_Registration_A);
            List<Tlv> tlvs_a = TlvListUtils.parse(data);
            TlvListUtils.getValuesDict(tlvs_a, dict);

            data = readElementaryFile(EF_Registration_B);
            List<Tlv> tlvs_b = TlvListUtils.parse(data);
            TlvListUtils.getValuesDict(tlvs_b, dict);

            data = readElementaryFile(EF_Registration_C);
            List<Tlv> tlvs_c = TlvListUtils.parse(data);
            TlvListUtils.getValuesDict(tlvs_c, dict);

            if(getCurrentCountry() == Country.SERBIA) {
                data = readElementaryFile(EF_SerbianRegis_D);
                List<Tlv> tlvs_d = TlvListUtils.parse(data);
                TlvListUtils.getValuesDict(tlvs_d, dict);
            }

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

    private Country getCurrentCountry() {
        return currentCountry;
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
