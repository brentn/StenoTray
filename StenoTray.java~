import java.awt.Dimension; 
import java.awt.Font;
import java.awt.Toolkit;
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

public class StenoTray extends JFrame {
    
    // I am guessing the plover config is located at this location on all systems
    private static final String CONFIG = System.getProperty("user.home")+"/.config/plover/stenotray.cfg";
    private static boolean DEBUG = false;
    // global variables
    private static String dictionaryFile = null;
    private static String logFile = System.getProperty("user.home")+"/.config/plover/plover.log";;
    private static Dictionary dictionary; // the main dictionary
    private int limit = 0; // limit the number of responses
    private boolean simplify = false;
    private Dimension screenSize;
    private JPanel panel = new JPanel();
    private JScrollPane scrollPane = new JScrollPane(panel);
    private Font font;
        
    public StenoTray() throws java.io.IOException {
        screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setPreferredSize(new Dimension(150, 375));
        this.setLocation(screenSize.width-150,screenSize.height-600);
        this.setFocusableWindowState(false);
        this.setAlwaysOnTop(true);
        panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
        loadDictionary();
        updateGUI("");
        tailLogFile();
    }
    
    public static void main(String[] args) throws java.io.IOException {        
        StenoTray tray = new StenoTray();
    }
    
    // PRIVATE METHODS
    
    private void updateGUI(String phrase) {
        if (phrase == null) return;
        panel.removeAll();
        repaint();
        if ((phrase.length() > 0) && (!phrase.equals("None"))) {
            int count = 0;
            for (Dictionary.Pair pair : dictionary.autoLookup(phrase)) {
                JLabel label = new JLabel(pair.translation()+" | "+simplify(pair.stroke()), JLabel.CENTER);
                label.setFont(font);
                label.setToolTipText(pair.stroke());
                panel.add(label);
                if (!(limit == 0) || (count < limit))
                    break;
                count++;
            }
        }
        this.add(scrollPane);
        this.validate();
        this.pack();
        this.setVisible(true);
    }
    
    private void tailLogFile() throws java.io.IOException {
        Reader fileReader = new FileReader(logFile);
        BufferedReader input = new BufferedReader(fileReader);
        String line = null;
        Translation translation = new Translation("");
        // position at the end of the file
        for (line = input.readLine(); line != null; line = input.readLine()) {};
        while (true) {
            if ((line = input.readLine()) != null) {
                parseLogLine(line, translation);
                updateGUI(translation.phrase());
                continue;
            }
            try {
                Thread.sleep(500L);
            } catch (InterruptedException x) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        input.close();
    }
    
    private void parseLogLine(String line, Translation translation) {
        if (line.indexOf("Translation") == -1) return;
        String stroke, prevStroke, undoStroke;
        String[] parts;
        if (line.indexOf("Translation((") == -1) { // old style log
            if (line.indexOf("*Translation") >= 0) { // delete stroke
                line = line.substring(line.indexOf("Translation")+12,line.length()-1);
                stroke = line.split(":")[1].trim();
                translation.delete(stroke);
                return;
            } else { 
                line = line.substring(line.indexOf("Translation")+12,line.length()-1);
                stroke = line.split(":")[1].trim();
                translation.add(stroke);
                return;
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
            return;
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
            if (left.contains("STP") && left.contains("KW")) 
                left = left.replace("STP","Z").replace("KW","");
            if (left.contains("S") && left.contains("KWR"))
                left = left.replace("S","J").replace("KWR","");
            if (left.contains("TP") && left.contains("KW"))
                left = left.replace("TP","G").replace("KW","");
            if (left.contains("T") && left.contains("K"))
                left = left.replace("T","D").replace("K","");
            if (left.contains("P") && left.contains("W"))
                left = left.replace("P","B").replace("W","");
            if (left.contains("H") && left.contains("R"))
                left = left.replace("H","L").replace("R","");
            if (left.contains("K") && left.contains("P"))
                left = left.replace("K","X").replace("P","");
            if (left.contains("K") && left.contains("R"))
                left = left.replace("K","C").replace("R","");
            if (left.contains("S") && left.contains("R"))
                left = left.replace("S","V").replace("R","");
            left = left.replace("TPH","N");
            left = left.replace("KWR","Y");
            left = left.replace("TP","F");
            left = left.replace("PH","M");
            left = left.replace("KW","Q");
            vowels = vowels.replace("EU","I");
            if (right.contains("PL") && right.contains("BG"))
                right = right.replace("PL","J").replace("BG","");
            if (right.contains("P") && right.contains("B"))
                right = right.replace("P","N").replace("B","");
            right = right.replace("PL","M");
            right = right.replace("BGS","X");
            right = right.replace("BG","K");
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
        String ploverConfig = System.getProperty("user.home")+"/.config/plover/plover.cfg";
        int fontSize = 12;
        In in;
        String line = "";
        String[] fields;
        if (new File(CONFIG).isFile()) {
            if (DEBUG) System.out.println("Loading config file ("+CONFIG+")...");
            in = new In(CONFIG);
            line = in.readLine();
            while (line != null) {
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
                line = in.readLine();
            }
            in.close();
        }
        font = new Font("Sans", Font.PLAIN, fontSize);
        if (new File(ploverConfig).isFile()) {
            if (DEBUG) System.out.println("reading Plover config ("+ploverConfig+")...");
            in = new In(ploverConfig);
            line = in.readLine();
            while ((line != null) && (dictionaryFile == null)) {
                fields = line.split("=");
                if (fields.length >= 2) {
                    if (fields[0].trim().equals("dictionary_file")) 
                        dictionaryFile = fields[1].trim();
                    if (fields[0].trim().equals("log_file")) 
                        logFile = fields[1].trim();
                }
                line = in.readLine();
            }
            in.close();
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