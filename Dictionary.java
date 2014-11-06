import java.util.Comparator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.TreeMap;
import java.util.NavigableMap;
import java.util.Map;
import java.util.Collection;


public class Dictionary {
    private MultiTreeMap<String> english = new MultiTreeMap<String>();                 // One translation can have multiple strokes, and keys are case insensitive.
    private NavigableMap<String, String> definitions = new TreeMap<String, String>();  // But one stroke can have only one translation

    // Constructor
    // Builds lookup and reverse-lookup symbol tables from .json dictionary file
    public Dictionary(List<String> dictionaryFiles) {
        for (String filename : dictionaryFiles) {
            loadDictionary(filename);
        }
    }

    public void loadDictionary(String filename) {
        if (filename == null || filename.equals("")) {
            throw new java.lang.IllegalArgumentException();
        }
        String stroke;
        String translation;
        String line = "";
        String[] fields;
        try {
        	BufferedReader file = new BufferedReader(
                    new InputStreamReader(new FileInputStream(filename), "utf-8")); 
        while ((line = readLine(file)) != null) {
            fields = line.split("\""); // TODO: This is buggy if text contains a quote. Should parse actual JSON.
            if ((fields.length) >= 3 && (fields[3].length() > 0)) {
                stroke = fields[1];
                translation = fields[3];
                english.putSingle(translation, stroke);  // This is the MultiTreeMap, so any duplicate keys get added to the list. This is technically buggy if you use multiple dictionaries and some entries are overwritten. TODO
                definitions.put(stroke, translation);    // This is the normal TreeMap, so any duplicate keys get overwritten by the last key.
            }
        }
        file.close();
        } catch (IOException e) {
            System.err.println("Could not find file: "+filename);
        }
    }
    
    // A class to allow managing stroke/translation pairs
    public class Pair {
        private String s;
        private String translation;
        public Pair(String st, String t) {
            s = st;
            translation = t;
        }
        public String translation() { return translation; }
        public String stroke() { return s; }
    }
 
 /*   
    // spell out a word letter-by-letter
    public String fingerspell(String word) {
        if (word.length() == 0) 
            throw new java.lang.IllegalArgumentException();
        String result = "";
        result = english.get("{&" + word.substring(0,1) + "}").shortest();
        for (int i = 1; i < word.length(); i++) {
            result += "/" + english.get("{&" + word.substring(i,i+1) + "}").shortest();
        }
        return result;
    }

    // look up shortest stroke for a word or phrase
    // taking into account multi-word strokes
    public String lookup(String phrase) {
        if (phrase.trim().length() == 0) 
            throw new java.lang.IllegalArgumentException();
        phrase = phrase.trim();
        String result = "";
        String word ;
        while (phrase.length() > 0) {
            word = english.longestPrefixOf(phrase);
            // if longest prefix is multiple words, then use it
            if (containsSpaces(word) && (phrase.charAt(word.length()) == ' ')) {
                result += english.get(word).shortest();
            } else {
                word = getFirstWord(phrase);
                if (english.contains(word))
                    result += english.get(word).shortest();
                else
                    result += buildUp(word);
            }
            phrase = phrase.substring(word.length(),phrase.length());
            // eliminate autospaces if necessary
            if ((phrase.length() > 0) && (phrase.length() == phrase.trim().length())) {
                result += "/TK-LS/";
            } else {
                phrase = phrase.trim();
                result += "/";
            }
        }
        return result;
    }
    
    // look up the English translation of a stroke or series of strokes
    public String translate(String strokes) {
        if (strokes.trim().length() == 0)
            throw new java.lang.IllegalArgumentException();
        strokes = strokes.trim();
        String result = "";
        String stroke, translation;
        Boolean insertSpace;
        while (strokes.length() > 0) {
            stroke = definitions.longestPrefixOf(strokes);
            if (stroke.length() == 0) return "";
            insertSpace = true;
            translation = definitions.get(stroke);
            if (strokes.equals(stroke)) {
                strokes = "";
            } else {
                strokes = strokes.substring(stroke.length()+1,strokes.length());
            }
            if (isMacro(translation)) {
                translation = translation.substring(1,translation.length()-1); // remove braces
                if ((translation.charAt(0) == '^') || (translation.charAt(0) == '&')) { 
                    translation = translation.substring(1);
                    result = result.trim();
                }
                if (translation.charAt(translation.length()-1) == '^') {
                    translation = translation.substring(0,translation.length()-1);
                    insertSpace = false;
                }
            }
            result += translation;
            if (insertSpace) result += " ";
        }
        return result;
    }
    
    // return all defined words/phrases that start with what we have already
    // returns an iterable sorted from shortest to longest
    // ie. fingerspell part of a word to autoLookup the correct stroke
//    public Iterable<Pair> autoLookup(String prefix) {
//        List<Pair> result = new ArrayList<Pair>();
//        if (prefix.length() <= 2) return result; // don't do lookup for short prefixes
//        for (String phrase : english.prefixMatch(prefix)) {
//            result.add(new Pair(english.get(phrase),phrase));
//        }
//        Collections.sort(result,new ByPhraseLength());
//        return result;
//    }
*/
    public Iterable<Pair> autoLookup(String stringPrefix, String strokePrefix) {
        List<Pair> result = new ArrayList<Pair>();
        if (stringPrefix.length() > 2) { // don't do lookup for short prefixes
            for (Collection<Tuple<String, String>> pairs : prefixMatch(english, stringPrefix).values()) {
                for (Tuple<String, String> pair : pairs) {
                    result.add(new Pair(pair.y,pair.x));
                }
            }
        }
        if (strokePrefix.length() >= 2) {
            if (strokePrefix.charAt(strokePrefix.length()-1) != '/')
                strokePrefix += "/"; // ensure the stroke is complete
            for (Map.Entry<String, String> pair : prefixMatch(definitions, strokePrefix).entrySet()) {
                result.add(new Pair(pair.getKey(), pair.getValue()));
            }
        }
        Collections.sort(result,new ByPhraseLength());
        return result;
    }
    
        
    // PRIVATE CLASSES AND METHODS
    
    private <T2> NavigableMap<String, T2> prefixMatch(NavigableMap<String, T2> m, String prefix) {
        return m.subMap(prefix, true, prefix+"\uffff", true);
    }
	
    private static class ByPhraseLength implements Comparator<Pair> {
        public int compare(Pair p1, Pair p2) {
            if ((p1 == null) || (p2 == null)) 
                throw new java.lang.IllegalArgumentException();
            if (p1.equals(p2)) return 0;
            int length1 = phraseLength(p1.translation());
            int length2 = phraseLength(p2.translation());
            if (length1 == length2) {
                length1 = strokeLength(p1.stroke());
                length2 = strokeLength(p2.stroke());
            }
            return (length1 - length2);
        }
    }
    
    private static class ByStrokeLength implements Comparator<Pair> {
        public int compare(Pair p1, Pair p2) {
            if ((p1 == null) || (p2 == null)) 
                throw new java.lang.IllegalArgumentException();
            if (p1.equals(p2)) return 0;
            int length1 = strokeLength(p1.stroke());
            int length2 = strokeLength(p2.stroke());
            if (length1 == length2) {
                length1 = phraseLength(p1.translation());
                length2 = phraseLength(p2.translation());
            }
            return (length1 - length2);
        }   
    }

    // count the number of slashes in a string
    private static int countSlashes(String s) {
        int result = 0;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == '/') result++;
        }
        return result;
    }

    // read a line from a file, catching errors
    private static String readLine(BufferedReader file) {
        String result = "";
        try {
            result = file.readLine();
        } catch (IOException e) {
            System.err.println("Error reading from file");
        }
        return result;
    }
/*    
    // use fingerspelling, prefixes and suffixes to build an efficient way to
    // stroke words not found in the dictionary
    private String buildUp(String word) {
        String prefix = "";
        String suffix = "";
        int len;
        Pair pair = findPrefix(word);
        if (pair != null) { 
            prefix = pair.stroke() + "/";
            len = pair.translation().length()-3; // -3 for meta characters {^}
            word = word.substring(len,word.length());
        }
        pair = findSuffix(word);
        if (pair != null) {
            suffix = "/" + pair.stroke();
            len = pair.translation().length()-3; // -3 for meta characters {^}
            word = word.substring(0,(word.length()-len)); 
        }
        String result = prefix + fingerspell(word) + suffix;
        return result;
    }
    
    // return the longest prefix stroke (ends with ^) 
    private Pair findPrefix(String word) {
        StrokeSet strokes;
        String paddedWord;
        for (int i = 1; i < word.length(); i++) {
            paddedWord = "{"+word.substring(0,word.length()-i)+"^}";
            strokes = english.get(paddedWord);
            if (strokes != null) 
                return new Pair(strokes,paddedWord);
        }
        return null;
    }
    
    // return the longest suffix (starts with ^)
    private Pair findSuffix(String word) {
        StrokeSet strokes;
        String paddedWord;
        for (int i = 1; i < word.length(); i++) {
            paddedWord = "{^"+word.substring(i,word.length())+"}";
            strokes = english.get(paddedWord);
            if (strokes != null)
                return new Pair(strokes,paddedWord);
        }
        return null;
    }
    
    private static boolean containsSpaces(String word) {
        for (int i = 0; i < word.length(); i++) {
            if (word.charAt(i) == ' ') return true;
        }
        return false;
    }
    
    private static String getFirstWord(String phrase) {
        String result = "";
        for (int i = 0; i < phrase.length(); i++) {
            if (phrase.charAt(i) == ' ') return result;
            else result += phrase.charAt(i);
        }
        return result;
    }
    
    private static Boolean isMacro(String word) {
        return ((word.charAt(0) == '{') && (word.charAt(word.length()-1) == '}'));
    }
*/    
    private static int strokeLength(String stroke) {
        return stroke.split("/").length * 100 + stroke.length();
    }
    
    private static int phraseLength(String word) {
        return word.split(" ").length * 10 + word.length();
    }
    
   
    public static void main(String[] args) {
	List<String> d = new ArrayList<String>();
	d.add("/home/brentn/Dropbox/Plover/gbDict2.json");
        Dictionary dictionary = new Dictionary(d);
//        System.out.println(dictionary.lookup("Unicyclist"));
//        System.out.println(dictionary.buildUp("interloping"));
//        System.out.println(dictionary.lookup("interloping is what it's all about"));
//        System.out.println(dictionary.translate("SPWR/HR*/O*/P*/-G/S/WHA/T-S/AUL/P/"));
        for (Pair p : dictionary.autoLookup("unic","HREUL")) {
            System.out.println(p.translation()+ " --> "+p.stroke());
        };
        //System.out.println(dictionary.shorter("HRAEUD/EUS/STK/SKWRE/PHEPB/-F/-T/SKWRUR"));
    }
}

    
