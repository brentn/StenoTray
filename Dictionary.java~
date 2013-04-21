import java.util.Comparator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Dictionary {
    
    private TST<StrokeSet> english = new TST<StrokeSet>();
    private TST<String> definitions = new TST<String>();

    // Constructor
    // Builds lookup and reverse-lookup symbol tables from .json dictionary file
    public Dictionary(String filename) {
        if (filename == null || filename.equals("")) {
            throw new java.lang.IllegalArgumentException();
        }
        String stroke;
        String translation;
        StrokeSet strokeSet;
        String line = "";
        String[] fields;
        In in = new In(filename);
        line = in.readLine();
        while (line != null) {
            fields = line.split("\"");
            if ((fields.length) >= 3 && (fields[3].length() > 0)) {
                stroke = fields[1];
                translation = fields[3];
                strokeSet = english.get(translation);
                if (strokeSet == null) 
                    strokeSet = new StrokeSet(stroke);
                else
                    strokeSet.add(stroke);
                english.put(translation, strokeSet);
                definitions.put(stroke, translation);
            }
            line = in.readLine();
        }
    }
    
    // A class to allow managing stroke/translation pairs
    public class Pair {
        private StrokeSet strokes;
        private String translation;
        public Pair(StrokeSet s, String t) {
            strokes = s;
            translation = t;
        }
        public String translation() { return translation; }
        public String stroke() { return strokes.shortest(); }
    }
    
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
    public Iterable<Pair> autoLookup(String prefix) {
        List<Pair> result = new ArrayList<Pair>();
        if (prefix.length() <= 2) return result; // don't do lookup for short prefixes
        for (String phrase : english.prefixMatch(prefix)) {
            result.add(new Pair(english.get(phrase),phrase));
        }
        Collections.sort(result,new ByPhraseLength());
        return result;
    }
    
        
    // PRIVATE CLASSES AND METHODS
    
    private class StrokeSet {
        private List<String> strokes;      
        public StrokeSet(String stroke) {
            strokes = new ArrayList<String>();
            add(stroke);
        }
        public void add(String stroke) { strokes.add(stroke); }
        public String shortest() {
            String result = null;
            for (String stroke : strokes) {
                if ((result == null) || (strokeLength(stroke) < strokeLength(result)))
                    result = stroke;
            }
            return result;
        }
    }
    
    private static class ByPhraseLength implements Comparator<Pair> {
        public int compare(Pair p1, Pair p2) {
            if ((p1 == null) || (p2 == null)) 
                throw new java.lang.IllegalArgumentException();
            if (p1.equals(p2)) return 0;
            int length1 = phraseLength(p1.translation());
            int length2 = phraseLength(p2.translation());
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
            return (length1 - length2);
        }   
    }


    
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
    
    private static int strokeLength(String stroke) {
        return stroke.split("/").length * 100 + stroke.length();
    }
    
    private static int phraseLength(String word) {
        return word.split(" ").length * 10 + word.length();
    }
    
    
    public static void main(String[] args) {
        Dictionary dictionary = new Dictionary("/home/brent/Dropbox/Plover/gbDict2.json");
        StdOut.println(dictionary.lookup("Unicyclist"));
        StdOut.println(dictionary.buildUp("interloping"));
        StdOut.println(dictionary.lookup("interloping is what it's all about"));
        StdOut.println(dictionary.translate("SPWR/HR*/O*/P*/-G/S/WHA/T-S/AUL/P/"));
        for (Pair p : dictionary.autoLookup("unic")) {
            StdOut.println(p.translation()+ " --> "+p.stroke());
        };
        //StdOut.println(dictionary.shorter("HRAEUD/EUS/STK/SKWRE/PHEPB/-F/-T/SKWRUR"));
    }
}

    