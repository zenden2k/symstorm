package ws.zenden.symstorm;

import java.io.*;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RoutingDumpReader {
    protected ArrayList<Entry> entries = new ArrayList<Entry>();
    
    static class Entry {
        public String rewriteCond;
        public String rewriteRule;
        public String controller;
    }
    
    public RoutingDumpReader(Reader reader2) throws IOException {
        BufferedReader reader = new BufferedReader(/*new FileReader(fileName)*/reader2);
        String line;

        Entry entry = null;

        while((line = reader.readLine()) != null){
            line = line.trim();
            if ( line.isEmpty() ) {
                continue;
            }
            if ( line.charAt(0) == '#' ) {  //skip comments
                continue;
            }

            if ( line.startsWith("RewriteCond") )  {
                String[] tokens = line.split("\\s+");
                if ( tokens.length >= 3 ) {
                    entry = new Entry();
                    entry.rewriteCond = tokens[2];
                }
            }

            if ( line.startsWith("RewriteRule") && entry != null ) {
                String[] tokens = line.split("\\s+");
                if ( tokens.length >= 4 ) {

                    String rewriteRuleParams = tokens[3];
                    Matcher matcher = Pattern.compile("_ROUTING_DEFAULTS__controller:([^\\],]+)").matcher(rewriteRuleParams);
                    
                    while(matcher.find()){
                        entry.controller = matcher.group(1);
                        entries.add(entry);
                    }
                    entry = null;
                }
            }
        }
    }

    public ArrayList<Entry> getEntries() {
        return this.entries;
    }
}
