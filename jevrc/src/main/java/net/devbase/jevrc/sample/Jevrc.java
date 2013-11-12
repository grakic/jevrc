
package net.devbase.jevrc.sample;

import java.util.List;
import java.util.Scanner;

import javax.smartcardio.Card;
import javax.smartcardio.CardException;
import javax.smartcardio.CardTerminal;
import javax.smartcardio.TerminalFactory;

import net.devbase.jevrc.EvrcCard;
import net.devbase.jevrc.EvrcInfo;

public class Jevrc {

    public static CardTerminal pickTerminal(List<CardTerminal> terminals) {
        if (terminals.size() > 1) {
            System.out.println("Available readers:\n");
            int c = 1;
            for (CardTerminal terminal : terminals) {
                System.out.format("%d) %s\n", c++, terminal);
            }

            @SuppressWarnings("resource")
			Scanner in = new Scanner(System.in);
            while (true) {
                System.out.print("Select number: ");
                System.out.flush();

                c = in.nextInt();
                if (c > 0 && c < terminals.size()) {
                    return terminals.get(c);
                }
            }
        } else {
            return terminals.get(0);
        }
    }

    public static void main(String[] args) {
        CardTerminal terminal = null;

        // get the terminal
        try {
            TerminalFactory factory = TerminalFactory.getDefault();
            terminal = pickTerminal(factory.terminals().list());

            System.out.println("Using reader   : " + terminal);
        } catch (CardException e) {
            System.err.println("Missing card reader.");
        }

        try {
            // establish a connection with the card
            Card card = terminal.connect("*");

            // read evrc data
            EvrcCard evrccard = new EvrcCard(card);
            EvrcInfo info = evrccard.readEvrcInfo();

            System.out.println(info);
            
        } catch (CardException e) {
            e.printStackTrace();
        }
    }
}
