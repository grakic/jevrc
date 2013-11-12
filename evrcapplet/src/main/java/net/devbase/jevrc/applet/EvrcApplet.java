package net.devbase.jevrc.applet;

import java.applet.Applet;
import java.util.List;

import javax.smartcardio.CardTerminal;
import javax.smartcardio.TerminalFactory;

import net.devbase.jevrc.Reader;
import net.devbase.jevrc.Reader.ReaderListener;
import net.devbase.jevrc.EvrcCard;
import net.devbase.jevrc.EvrcInfo;

import netscape.javascript.JSObject;

@SuppressWarnings("restriction")
public class EvrcApplet extends Applet implements ReaderListener {

    private static final long serialVersionUID = 8162139504713679711L;

    JSObject window = null;
    EvrcCard card = null;
    String removedCallback = "removed";
    String insertedCallback = "inserted";

    @Override
    public void init() {
        // pick the first terminal
        CardTerminal terminal = null;
        try {
            TerminalFactory factory = TerminalFactory.getDefault();
            List<CardTerminal> terminals = factory.terminals().list();
            terminal = terminals.get(0);
        } catch(Exception e) {
            stop();
        }

        // build a reader
        Reader reader = new Reader(terminal);
        reader.addCardListener(this);

        // get callbacks
        removedCallback = getParameter("RemovedCallback");
        insertedCallback = getParameter("InsertedCallback");
    }

    /* export public function as the Mozilla Bug 606737 workaround */
    public void setupJSObject() {
        if((window = JSObject.getWindow(this)) == null) {
            stop();
        }
    }

    @Override
    public void start() {
        setupJSObject();
    }

    public void inserted(EvrcCard card) {
        this.card = card;

        showStatus("Card inserted.");
        try {
            EvrcInfo info = card.readEvrcInfo();
            String infoJson = info.toJSON().toString();

            window.call(insertedCallback, new Object[] {infoJson});
        } catch (Exception e) {
            stop();
        }
    }

    public void removed() {
        showStatus("Card removed.");
        window.call(removedCallback, null);
    }
}
