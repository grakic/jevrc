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
    String terminalName = null;
    String lastError = null;
    String removedCallback = "removed";
    String insertedCallback = "inserted";
    String errorCallback = "onerror";

    @Override
    public void init() {

        // get callbacks
        removedCallback = getParameter("RemovedCallback");
        insertedCallback = getParameter("InsertedCallback");
        errorCallback = getParameter("ErrorCallback");

        // pick the first terminal
        CardTerminal terminal = null;
        try {
            TerminalFactory factory = TerminalFactory.getInstance("PC/SC", null);
            List<CardTerminal> terminals = factory.terminals().list();
            terminal = terminals.get(0);
            terminalName = terminal.getName();
        } catch(Exception e) {
            notifyError(e.getMessage());
            return;
        }

        // build a reader
        Reader reader = new Reader(terminal);
        reader.addCardListener(this);
    }

    /* export public function as the Mozilla Bug 606737 workaround */
    public void setupJSObject() {
        if((window = JSObject.getWindow(this)) != null) {
            notifyError(null);
        }
    }

    @Override
    public void start() {
        if(window == null) {
            setupJSObject();
        }
    }

    private void notifyError(String error) {
        showStatus("Error.");
        if (error != null) {
            lastError = error;
        }
        if (window != null && lastError != null) {
            window.call(errorCallback, new Object[] {lastError});
            lastError = null;
        }
    }

    public String getTerminalName() {
        return terminalName;
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
