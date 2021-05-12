import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.json.JsonArray;
import javax.json.JsonReader;
import javax.json.Json;
import javax.json.JsonObject;
import java.util.ListIterator;
import java.io.StringReader;

public class StenoTray extends JFrame {
    static String mkPath(String path1, String... paths)
    {
        StringBuilder sb = new StringBuilder(path1);
        for (String p : paths)
        {
            sb.append(PSEP + p);
        }
        return sb.toString();
    };

    // find the default location of the plover config directory
    private static final String PSEP       = System.getProperty("file.separator");
    private static final String UHOME      = System.getProperty("user.home");
    private static final String PLOVER_DIR;
    static {
        String[] innerDirsWin = {"AppData", "Local", "plover", "plover"};
        String[] innerDirsOther = {".config", "plover"};
        String[] innerDirs;
	String osName = System.getProperty("os.name");
        if (osName.startsWith("Windows")) {
            PLOVER_DIR = mkPath(UHOME, "AppData", "Local", "plover", "plover");
        } else if (osName.startsWith("Mac")) {
            PLOVER_DIR = mkPath(UHOME, "Library", "Application Support", "plover");
        } else {
            PLOVER_DIR = mkPath(UHOME, ".config", "plover");
        }
    }

    private static final String CONFIG_DIR = mkPath(PLOVER_DIR, "stenotray.cfg");
    private static boolean DEBUG = false;

    // global variables
    private static List<String> dictionaryFiles = new ArrayList<String>();
    private static String logFile = mkPath(PLOVER_DIR, "plover.log");
    private static Dictionary dictionary; // the main dictionary
    private int limit = 0; // limit the number of responses
    private boolean simplify = false;
    private Dimension screenSize;
    private JPanel mainPanel = new JPanel();
    private JScrollPane scrollPane = new JScrollPane(mainPanel);
    private Font font, strokeFont;

    public StenoTray() throws java.io.IOException {
        screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        final int prefSizeX = 300;
        final int prefSizeY = 650;
        final int taskBarSize = 56;

        this.setPreferredSize(new Dimension(prefSizeX, prefSizeY));
        this.setLocation(screenSize.width-prefSizeX,screenSize.height-prefSizeY-taskBarSize);
        this.setFocusableWindowState(false);
        this.setAlwaysOnTop(true);
        mainPanel.setLayout(new BorderLayout());
        loadDictionary();
        updateGUI("","");
        tailLogFile();
    }

    public static void main(String[] args) throws java.io.IOException {
        StenoTray tray = new StenoTray();
    }

    
    // PRIVATE METHODS

    String colorSteno(String text)
    {
        // Let's make this a neat little state machine. Now if only it were fast...
        StringBuilder sb = new StringBuilder("<html>");
        int state = 0;
        String s0 = "/#";
        String s1 = "STPHKWR1234JGNYDBLFMQXCV";
        String s2 = "AO5";
        String s3 = "*-0";
        String s4 = "EUI";
        String s5 = "FPLTDRBGSZ6789JXNMK";
        for (char c : text.toCharArray()) {
            switch (state) {
                case 0 : if (s1.indexOf(c) != -1) {
                             state = 1;
                             sb.append(" <font color=\"red\">");
                         } else if (s2.indexOf(c) != -1) {
                             state = 2;
                             sb.append(" <font color=\"white\" bgcolor=\"red\">");
                         } else if (s3.indexOf(c) != -1){
                             sb.append(" ");
                             state = 3;
                         } else if (s4.indexOf(c) != -1) {
                             state = 4;
                             sb.append(" <font color=\"white\" bgcolor=\"blue\">");
                         } else if (s0.indexOf(c) != -1) {
                             sb.append(" ");
                         }
                         break;
                case 1 : if (s2.indexOf(c) != -1) {
                             state = 2;
                             sb.append("</font><font color=\"white\" bgcolor=\"red\">");
                         } else if (s3.indexOf(c) != -1) {
                             state = 3;
                             sb.append("</font>");
                         } else if (s4.indexOf(c) != -1) {
                             state = 4;
                             sb.append("</font><font color=\"white\" bgcolor=\"blue\">");
                         } else if (s0.indexOf(c) != -1) {
                             state = 0;
                             sb.append("</font> ");
                         }
                         break;
                case 2 : if (s3.indexOf(c) != -1) {
                             state = 3;
                             sb.append("</font>");
                         } else if (s4.indexOf(c) != -1) {
                             state = 4;
                             sb.append("</font><font color=\"white\" bgcolor=\"blue\">");
                         } else if (s5.indexOf(c) != -1) {
                             state = 5;
                             sb.append("</font><font color=\"blue\">");
                         } else if (s0.indexOf(c) != -1) {
                             state = 0;
                             sb.append("</font> ");
                         }
                         break;
                case 3 : if (s4.indexOf(c) != -1) {
                             state = 4;
                             sb.append("<font color=\"white\" bgcolor=\"blue\">");
                         } else if (s5.indexOf(c) != -1) {
                             state = 5;
                             sb.append("<font color=\"blue\">");
                         } else if (s0.indexOf(c) != -1) {
                             state = 0;
                             sb.append(" ");
                         }
                         break;
                case 4 : if (s5.indexOf(c) != -1) {
                             state = 5;
                             sb.append("</font><font color=\"blue\">");
                         } else if (s0.indexOf(c) != -1) {
                             state = 0;
                             sb.append("</font> ");
                         }
                         break;
                case 5 : if (s0.indexOf(c) != -1) {
                             state = 0;
                             sb.append("</font> ");
                         }
                         break;
                default: throw new IllegalArgumentException("State machine in impossible state");
            }
            sb.append(c);
        }
        if (state == 2 || state == 4 || state == 5)
            sb.append("</font>");
        sb.append("</html>");       
        return sb.toString(); 
    }

    private void updateGUI(String phrase, String stroke) {
        if (stroke == null) return;
        if (phrase == null) phrase = "";
        mainPanel.removeAll();
        repaint();
        JPanel list = new JPanel();
        list.setLayout(new BoxLayout(list, BoxLayout.PAGE_AXIS));
        mainPanel.add(list, BorderLayout.NORTH);
        int count = 1;
        for (Dictionary.Pair pair : dictionary.autoLookup(phrase, stroke)) {
            JPanel line = new JPanel();
            line.setLayout(new FlowLayout(FlowLayout.LEFT,5,0));
            JLabel translationLabel = new JLabel(pair.translation());        
            JLabel strokeLabel = new JLabel(colorSteno(simplify(pair.stroke())));
            //JLabel strokeLabel = new JLabel(simplify(pair.stroke()));
            translationLabel.setFont(font);
            strokeLabel.setFont(strokeFont);
            line.add(translationLabel);
            line.add(strokeLabel);
            list.add(line, BorderLayout.NORTH);
            if (count++ == limit)
                break;
        }
        this.setTitle(phrase+"  "+stroke);
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
        Translation translation = new Translation("");
        for (line = input.readLine(); line != null; line = input.readLine()) {};  // position at the end of the file
        while (true) {
            if ((line = input.readLine()) != null) {
                stenoStroke = parseLogLine(line, translation);
                String phrase = translation.phrase();
                if (input.ready())
                    continue;
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
        String stroke = "", prevStroke, undoStroke;
        String[] parts;
        if (line.indexOf("Translation((") == -1 || true) { // old style log
            if (line.indexOf("*Translation") >= 0) { // delete stroke
                line = line.substring(line.indexOf("Translation")+12,line.length()-1);
                translation.delete(line.split(":",2)[1].trim());
                String rawstroke = line.split(":",2)[0].trim();
                rawstroke = rawstroke.substring(1,rawstroke.length()-1);
                for(String str : rawstroke.split(",")) {
                    if (stroke != "")
                        stroke += "/";
                    str = str.trim();
                    stroke += str.substring(1,str.length()-1);
                }
                return stroke;
            } else { 
                line = line.substring(line.indexOf("Translation")+12,line.length()-1);
                translation.add(   line.split(":",2)[1].trim());
                String rawstroke = line.split(":",2)[0].trim();
                rawstroke = rawstroke.substring(1,rawstroke.length()-1);
                for(String str : rawstroke.split(",")) {
                    if (stroke != "")
                        stroke += "/";
                    str = str.trim();
                    stroke += str.substring(1,str.length()-1);
                }
                if (DEBUG) System.out.println("stroke:"+stroke);
                return stroke;
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
            return translation.phrase();
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
            // Grab the biggest clusters first, then in steno order
            left = left
                    .replace("STKPW","Z")
                    .replace("SKWR",  "J")
                    .replace("TKPW", "G")
                    .replace("TPH", "N")
                    .replace("KWR", "Y")
                    .replace("SR", "V")
                    .replace("TK","D")
                    .replace("TP", "F")
                    .replace("KP", "X")
                    .replace("KW", "Q")
                    .replace("KR", "C")
                    .replace("PW","B")
                    .replace("PH", "M")
                    .replace("HR","L");

            vowels = vowels
                    .replace("EU","I");

            right = right
                    .replace("PBLG","J")
                    .replace("BGS", "X")
                    .replace("PB", "N")
                    .replace("PL", "M")
                    .replace("BG", "K");

            result += left + vowels + right + "/";
        }
        return result.substring(0,result.length()-1);
    }
    
    private void loadDictionary() throws java.io.FileNotFoundException {
        readConfig();
        if (DEBUG) System.out.println("Loading "+Integer.toString(dictionaryFiles.size())+" dictionaries...");
        dictionary = new Dictionary(dictionaryFiles);
        if (DEBUG) System.out.println("Dictionaries loaded");
    }
    
    private void readConfig() throws java.io.FileNotFoundException {
    	GraphicsEnvironment g = GraphicsEnvironment.getLocalGraphicsEnvironment();
        String ploverConfig = mkPath(PLOVER_DIR, "plover.cfg");
        int fontSize = 12;
    	// Default font
        String fontName = "Sans";
        // Use "Segoe UI Symbol" font flag
        boolean useSegoe = false;
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
        try {
        	for (String ffname : g.getAvailableFontFamilyNames()) {
                if (ffname.equals("Segoe UI Symbol")) {
                	useSegoe = true;
                	break;
                }
            }
            if (useSegoe) {
            	fontName = "Segoe UI Symbol";
            }
        } catch (Exception e) {
        	// Do nothing if we have issues reading fonts
        	if (DEBUG) {
        		e.printStackTrace();
        	}
        }
        
        font = new Font(fontName, Font.BOLD, fontSize);
        strokeFont = new Font(fontName, Font.BOLD, (fontSize));
        if (new File(ploverConfig).isFile()) {
            if (DEBUG) System.out.println("reading Plover config ("+ploverConfig+")...");
            try {
                BufferedReader pConfig = new BufferedReader(new FileReader(ploverConfig));
                while (((line = pConfig.readLine()) != null)) {
                    fields = line.split("=");
                    if (fields.length >= 2) {
                        if (fields[0].trim().equals("dictionaries")) {
                            JsonReader reader = Json.createReader(new StringReader(fields[1].trim()));
                            JsonArray list = reader.readArray();
                            ListIterator l = list.listIterator();
                            while ( l.hasNext() ) {
                                JsonObject j = (JsonObject) l.next();
                                dictionaryFiles.add(mkPath(PLOVER_DIR, j.getString("path")));
                            }
                        }
                        if (fields[0].trim().equals("dictionary_file"))
                            dictionaryFiles.add(fields[1].trim());
                        if (fields[0].trim().equals("log_file"))
                            logFile = fields[1].trim();
                    }
                }
                pConfig.close();
            } catch (IOException e) {
                System.err.println("Error reading Plover configuration file");
            }
            if (dictionaryFiles == null)
                throw new java.lang.IllegalArgumentException("Unable to locate Plover dictionary file(s)");
            if (logFile == null)
                throw new java.lang.IllegalArgumentException("Unable to locate Plover Log file");
        } else {
            throw new java.io.FileNotFoundException("Cannot locate plover config file: " + ploverConfig.toString());
        }
    }

    private class Translation {
        private String[] history;
        private int histPointer;
        private static final int HIST_SIZE = 50;
        private String phrase = null;
        private boolean glue = false;
        private boolean joinEnd = false;
        public Translation(String input) {
            history = new String[HIST_SIZE];
            for (int i=0; i < HIST_SIZE; i++)
                history[i]="00";
            histPointer = 0;
            phrase = processAttributes(input.trim());
        }
        public String phrase() { return phrase; }
        public void add(String stroke) {
            String histBits;
            if (stroke == null || stroke.length() == 0 || stroke.equals("None")) {
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
            histBits = (glue)?"1":"0";
            histBits += (joinEnd)?"1":"0";
            history[histPointer] = histBits+phrase;
            histPointer = (histPointer + 1) % HIST_SIZE;
            if (DEBUG) System.out.println("->"+stroke+" ("+history[(histPointer+HIST_SIZE-1) % HIST_SIZE]+")");
        }
        public void add(String undo, String stroke, String prev) {
            if (undo != null && undo.length() > 0) delete(undo);
            if (stroke != null && stroke.length() > 0) add(stroke);
        }
        public void delete(String stroke) {
            histPointer = (histPointer+HIST_SIZE - 2) % HIST_SIZE;
            glue = (history[histPointer].charAt(0) == '1');
            joinEnd = (history[histPointer].charAt(1) == '1');
            phrase = history[histPointer].substring(2);
            histPointer = (histPointer + 1) % HIST_SIZE;
            if (DEBUG) System.out.println("<-"+stroke+" ("+history[(histPointer) % HIST_SIZE]+")");
        }
        private String processAttributes(String s) {
            glue = false;
            joinEnd = false;
            if (s == null || s.length() == 0) return null;
            if ((s.charAt(0) != '{') || (s.charAt(s.length()-1) != '}')) return s;
            int trimStart = 1;
            int trimEnd = 1;
            glue=hasGlue(s);
            joinEnd=joinEnd(s);
            if (glue) trimStart++;
            if (joinStart(s)) trimStart++;
            if (joinEnd) trimEnd++;
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
        private boolean joinEnd(String s) {
            if (s == null || s.length()<2) return false;
            return (s.charAt(s.length()-2) == '^');
        }
    }
}
