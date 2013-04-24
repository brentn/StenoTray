import java.awt.Dimension; 
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.Color;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.GridBagConstraints;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.BoxLayout;
import java.io.File;
import java.io.Reader;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Vector;
import java.util.Set;
import java.util.HashSet;

public class StenoTray extends JFrame {
    static String mkPath(String path1, String... paths)
    {
        StringBuilder sb = new StringBuilder(path1);
        for (String p : paths)
        {
            sb.append(PSEP + p);
        }
        return sb.toString();
    }


    // I am guessing the plover config is located at this location on all systems
    private static final String PSEP       = System.getProperty("file.separator");
    private static final String UHOME      = System.getProperty("user.home");
    private static final String PLOVER_DIR;
    static {
        String[] innerDirsWin = {"AppData", "Local", "plover", "plover"};
        String[] innerDirsOther = {".config", "plover"};
        String[] innerDirs;
        if (System.getProperty("os.name").startsWith("Windows")) {
            PLOVER_DIR = mkPath(UHOME, "AppData", "Local", "plover", "plover");
        } else {
            PLOVER_DIR = mkPath(UHOME, ".config", "plover");
        }
    }

    private static final String CONFIG_DIR = mkPath(PLOVER_DIR, "stenotray.cfg");
    private static boolean DEBUG = false;
    // global variables
    private static String dictionaryFile = mkPath(PLOVER_DIR, "dict.json");

    private static String logFile = mkPath(PLOVER_DIR, "plover.log");
    private static Dictionary dictionary; // the main dictionary
    private int limit = 0; // limit the number of responses
    private boolean simplify = false;
    private Dimension screenSize;
    private JPanel panel = new JPanel();
    private JScrollPane scrollPane = new JScrollPane(panel);
    private Font font, strokeFont;

    public StenoTray() throws java.io.IOException {
        screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        final int prefSizeX = 400;
        final int prefSizeY = 650;
        final int taskBarSize = 56;

        this.setPreferredSize(new Dimension(prefSizeX, prefSizeY));
        this.setLocation(screenSize.width-prefSizeX,screenSize.height-prefSizeY-taskBarSize);
        this.setFocusableWindowState(false);
        this.setAlwaysOnTop(true);
        panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
        loadDictionary();
        updateGUI("","");
        tailLogFile();
    }

    public static void main(String[] args) throws java.io.IOException {
        StenoTray tray = new StenoTray();
    }

    // PRIVATE METHODS

    private static boolean hasGrammaticalEnding(String trn, String phrase)
    {
        // is plural/gerund/future/possessive/... form of the word
        String[] grammaticalEndings = { "s", "ing", "'ll", "'s", "'d", "'ve" };
        return hasEnding(trn, phrase, grammaticalEndings);
    }

    private static boolean hasEnding(String trn, String phrase, String[] endings)
    {
        for (String ending : endings) {
            if (trn.equals(phrase + ending))    return true;
        }
        return false;
    }

    private void setLabelVisualStyle(JLabel label, String phrase, String trn, Dictionary.Pair pair, Font thisFont)
    {
            label.setFont(thisFont);

            // starts with the full phrase, then a space or a special sequence
            if (trn.startsWith(phrase + " ") || trn.startsWith(phrase + "{")) {
                label.setOpaque(true);
                label.setBackground(new Color(0xaaffaa));
            }

            if (hasGrammaticalEnding(trn, phrase)) {
                label.setOpaque(true);
                label.setBackground(new Color(0xaaaaff));
            }

            label.setToolTipText(pair.stroke());
    }

    String colorSteno(String text)
    {
        // 0 : left hand side, 1 : lhs vowel, 2 : rhs vowel, 3: right hand side
        int prevPart = 0;
        StringBuilder sb = new StringBuilder("<html><font color=\"red\">");

        String lhsVowels = "AO";
        String rhsVowels = "EUI";

        String bgColors[] = { "",                "bgcolor=\"red\"", "bgcolor=\"blue\"",  "",               "" };
        String fgColors[] = { "color=\"red\"",   "color=\"white\"",  "color=\"white\"",  "color=\"blue\"", "" };

        for (char c : text.toCharArray())
        {
            int currPart = prevPart;
            boolean shouldColor = true;

            if (c == '/') {
                currPart = 0;
                shouldColor = false;
            } else if (c == '*') {
//                currPart = 2;
                shouldColor = false;
            } else if (c == '-') {
                currPart = 3;
                shouldColor = false;
            } else if (lhsVowels.indexOf(c) != -1) {
                currPart = 1;
            } else if (rhsVowels.indexOf(c) != -1) {
                currPart = 2;
            } else if ((currPart == 1 || currPart == 2) && lhsVowels.indexOf(c) == -1 && rhsVowels.indexOf(c) == -1) {
                currPart = 3;
            }

            if (currPart == prevPart ) {
                sb.append(c);
                continue;
            }

            if (shouldColor) {
                sb.append(String.format("</font><font %s %s>%c", fgColors[currPart], bgColors[currPart], c));
            } else {
                sb.append(String.format("</font>%c<font %s %s>", c, fgColors[currPart], bgColors[currPart]));
            }

            prevPart = currPart;
        }

        sb.append("</font></html>");
        return sb.toString();
    }

    public static String strJoin(String[] aArr, String sSep) {
        StringBuilder sbStr = new StringBuilder();
        for (int i = 0, il = aArr.length; i < il; i++) {
            if (i > 0)
                sbStr.append(sSep);
            sbStr.append(aArr[i]);
        }
        return sbStr.toString();
    }

    private void updateGUI(String phrase, String stroke) {
        if (stroke == null) return;
        if (phrase == null) phrase = "";
        panel.removeAll();
        repaint();

        panel.setLayout(new BorderLayout());
        JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayout(0,2));
        panel.add(panel2, BorderLayout.NORTH);
        String fontStyle;
        String phraseRest;
        String label2Text;
        String trn;
        for (Dictionary.Pair pair : dictionary.autoLookup(phrase, stroke)) {
            JLabel label = new JLabel(pair.translation());        
            label2Text = colorSteno(simplify(pair.stroke()));
            JLabel label2 = new JLabel(label2Text);

            setLabelVisualStyle(label2, phrase, "", pair, strokeFont);

            panel2.add(label);
            panel2.add(label2);
        }

        this.add(scrollPane);
        this.validate();
        this.pack();
        this.setVisible(true);
    }


    private void tailLogFile() throws java.io.IOException {
        Reader fileReader = new FileReader(logFile);
        final BufferedReader input = new BufferedReader(fileReader);
        String line = null;
        String stenoStroke;
        final Translation translation = new Translation("");
        for (line = input.readLine(); line != null; line = input.readLine()) {};  // position at the end of the file
        while (true) {
            if ((line = input.readLine()) != null) {
                stenoStroke = parseLogLine(line, translation);
                String phrase = translation.phrase();
                updateGUI(phrase, stenoStroke);
                continue;
            }
            try {
                Thread.sleep(100L);
            } catch (InterruptedException x) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        input.close();
    }
    
    private String parseLogLine(String line, Translation translation) {
        if (line.indexOf("Translation") == -1) return "";
        String stroke, prevStroke, undoStroke;
        String[] parts;
        if (line.indexOf("Translation((") == -1) { // old style log
            if (line.indexOf("*Translation") >= 0) { // delete stroke
                line = line.substring(line.indexOf("Translation")+12,line.length()-1);
                stroke = line.split(":")[1].trim();
                translation.delete(stroke);
                return stroke;
            } else { 
                line = line.substring(line.indexOf("Translation")+12,line.length()-1);
                stroke = line.split(":")[1].trim();
                translation.add(stroke);
                if (DEBUG) System.out.println("stroke:"+line.split(":")[0].trim());
                return line.split(":")[0].trim();
            }
        } else { // new log style 
            line = line.substring(line.indexOf("(")+1);
            parts = line.split("],");
            if (parts[0].indexOf(":") == -1) {
                undoStroke = null;
            } else {
                undoStroke = parts[0].split(":")[1].trim();
                undoStroke = undoStroke.substring(0,undoStroke.length()-1);
            }
            if (parts[1].indexOf(":") == -1) {
                stroke = null;
            } else {
                stroke = parts[1].split(":")[1].trim();
                stroke = stroke.substring(0,stroke.length()-1);
            }
            if (parts[2].indexOf(":") == -1) {
                prevStroke = null;
            } else {
                prevStroke = parts[2].split(":")[1].trim();
                prevStroke = prevStroke.substring(0,prevStroke.length()-1);
            }
            translation.add(undoStroke,stroke,prevStroke);
            return stroke;
        }
    }
    
    private String simplify(String strokes){
        if (!simplify) return strokes;
        String VOWELS = "AOEU-*";
        String result = "";
        for (String stroke : strokes.split("/")) {
            String left = "";
            String right = "";
            String vowels = "";
            // divide the stroke into parts
            int firstVowel = -1;
            int lastVowel = -1;
            for (int i=0; i < stroke.length(); i++) {
                if (VOWELS.indexOf(stroke.charAt(i)) >= 0) {
                    if (firstVowel == -1) firstVowel = i;
                    lastVowel = i+1;
                }
            }
            if (firstVowel >= 0) {
                left = stroke.substring(0,firstVowel);
                vowels = stroke.substring(firstVowel,lastVowel);
                right = stroke.substring(lastVowel);
            } else {
                left = stroke;
            }
            left = left.replace("STKPW","Z");
            if (left.contains("S") && left.contains("K") && left.contains("W") && left.contains("R"))
                left = left.replace("S","J").replace("K","").replace("W","").replace("R","");
            left = left.replace("TKPW","G");
            if (left.contains("T") && left.contains("P") && left.contains("H"))
                left = left.replace("T","N").replace("P","").replace("H","");
            if (left.contains("K") && left.contains("W") && left.contains("R"))
                left = left.replace("K","Y").replace("W","").replace("R","");
            if (left.contains("T") && left.contains("P"))
                left = left.replace("T","F").replace("P","");
            if (left.contains("P") && left.contains("H"))
                left = left.replace("P","M").replace("H","");
            if (left.contains("K") && left.contains("W"))
                left = left.replace("K","Q").replace("W","");
            left = left.replace("TK","D");
            left = left.replace("PW","B");
            left = left.replace("HR","L");
            if (left.contains("K") && left.contains("P"))
                left = left.replace("K","X").replace("P","");
            if (left.contains("K") && left.contains("R"))
                left = left.replace("K","C").replace("R","");
            if (left.contains("S") && left.contains("R"))
                left = left.replace("S","V").replace("R","");
            vowels = vowels.replace("EU","I");
            right = right.replace("PBLG","J");
            if (right.contains("B") && right.contains("G") && right.contains("S"))
                right = right.replace("B","X").replace("G","").replace("S","");
            right = right.replace("PB","N");
            if (right.contains("P") && right.contains("L"))
                right = right.replace("P","M").replace("L","");
            if (right.contains("B") && right.contains("G"))
                right = right.replace("B","K").replace("G","");
            result += left+vowels+right+"/";
        }
        return result.substring(0,result.length()-1);
    }
    
    private void loadDictionary() throws java.io.FileNotFoundException {
        readConfig();
        if (DEBUG) System.out.println("Loading dictionary ("+dictionaryFile+")...");
        dictionary = new Dictionary(dictionaryFile);
        if (DEBUG) System.out.println("Dictionary loaded");
    }
    
    private void readConfig() throws java.io.FileNotFoundException {
        String ploverConfig = mkPath(PLOVER_DIR, "plover.cfg");
        int fontSize = 12;
        String line = "";
        String[] fields;
        if (new File(CONFIG_DIR).isFile()) {
            if (DEBUG) System.out.println("Loading config file ("+CONFIG_DIR+")...");
            try {
                BufferedReader stConfig = new BufferedReader(new FileReader(CONFIG_DIR));
                while ((line = stConfig.readLine()) != null) {
                    if (line.contains("=")) {
                        fields = line.split("=");
                        if (fields[0].trim().equals("PLOVER_CONFIG"))
                            ploverConfig = fields[1].trim();
                        else if (fields[0].trim().equals("LIMIT_RESULTS"))
                            limit = Integer.parseInt(fields[1].trim());
                        else if (fields[0].trim().equals("SIMPLIFY"))
                            simplify = (fields[1].trim().equals("true"));
                        else if (fields[0].trim().equals("FONT_SIZE"))
                            fontSize = Integer.parseInt(fields[1].trim());
                        else if (fields[0].trim().equals("DEBUG"))
                            DEBUG = (fields[1].trim().equals("true"));
                    }
                }
                stConfig.close();
            } catch (IOException e) {
                System.err.println("Error reading config file: "+CONFIG_DIR);
            }
        }
        font = new Font("Sans", Font.PLAIN, fontSize);
        strokeFont = new Font("Consolas", Font.PLAIN, (fontSize+10));
        if (new File(ploverConfig).isFile()) {
            if (DEBUG) System.out.println("reading Plover config ("+ploverConfig+")...");
            try {
                BufferedReader pConfig = new BufferedReader(new FileReader(ploverConfig));
                while (((line = pConfig.readLine()) != null) && (dictionaryFile == null)) {
                    fields = line.split("=");
                    if (fields.length >= 2) {
                        if (fields[0].trim().equals("dictionary_file"))
                            dictionaryFile = fields[1].trim();
                        if (fields[0].trim().equals("log_file"))
                            logFile = fields[1].trim();
                    }
                }
                pConfig.close();
            } catch (IOException e) {
                System.err.println("Error reading Plover configuration file");
            }
            if (dictionaryFile == null)
                throw new java.lang.IllegalArgumentException("Unable to locate Plover dictionary file");
            if (logFile == null)
                throw new java.lang.IllegalArgumentException("Unable to locate Plover Log file");
        } else {
            throw new java.io.FileNotFoundException("Cannot locate plover config file");
        }
    }

    private class Translation {
        private String prevPhrase = null;
        private String phrase = null;
        private boolean glue = false;
        private boolean joinEnd = false;
        public Translation(String input) {
            phrase = processAttributes(input.trim());
        }
        public String phrase() { return phrase; }
        public void add(String stroke) {
            prevPhrase = phrase;
            if (stroke == null || stroke.equals("None")) {
                phrase = null;
                glue = false;
                joinEnd = false;
            } else {
                if ((joinStart(stroke)) || (glue && hasGlue(stroke))) {
                    phrase += processAttributes(stroke);
                } else {
                    phrase = processAttributes(stroke);
                }
            }
            if (DEBUG) System.out.println("->"+stroke+" ("+prevPhrase+")");
        }
        public void add(String undo, String stroke, String prev) {
            prevPhrase = phrase;
            delete(undo);
            add(stroke);
        }
        public void delete(String stroke) {
            if (prevPhrase == null || prevPhrase == "None") {
                stroke = null;
                prevPhrase = null;
                return;
            }
            if ((joinStart(stroke)) || (glue && hasGlue(stroke)) && (prevPhrase != null)) {
                stroke = processAttributes(stroke);
                if ((stroke.length()+1) > phrase.length())
                    phrase = null;
                else
                    phrase = phrase.substring(0,phrase.length()-(stroke.length()+1));
                prevPhrase = null;
            } else {
                phrase = prevPhrase;
                prevPhrase = null;
            }
        }
        private String processAttributes(String s) {
            if (s == null || s.length() == 0) return null;
            if ((s.charAt(0) != '{') || (s.charAt(s.length()-1) != '}')) return s;
            int trimStart = 1;
            int trimEnd = 1;
            if (hasGlue(s)) {
                glue=true;
                trimStart++;
            } else if (joinStart(s)) {
                trimStart++;
            }
            if (s.charAt(s.length()-1) == '^') {
                joinEnd=true;
                trimEnd++;
            }
            return s.substring(trimStart,s.length()-trimEnd);
        }
        private boolean hasGlue(String s) {
            if (s == null || s.length()<2) return false;
            return (s.charAt(1) == '&');
        }
        private boolean joinStart(String s) {
            if (s == null || s.length()<2) return false;
            return (s.charAt(1) == '^');
        }
    }
}
