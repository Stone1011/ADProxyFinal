package ad.blocker.filter;

import static java.lang.Character.isDigit;
import static java.lang.Character.isLetter;

import android.util.Log;

import androidx.core.util.Pair;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

public class Filter
{
    public final String scriptStart = "<script>\nfunction AD_Proxy_block() {\n";
    public final String scriptEnd = "}\nwindow.addEventListener('load', AD_Proxy_block); \ndocument.addEventListener('DOMSubtreeModified', AD_Proxy_block); \n</script>\n";

    private Set <BlackElem> blackLists;
    private Set <RuleElem> rules;

    public Filter()
    {
        blackLists = new HashSet <>();
        rules = new HashSet <>();
    }

    public boolean init(final String path)
    {
        File file = new File(path);
        try
        {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line;
            line = reader.readLine();
            while(line != null)
            {
                if(line.isEmpty())
                {
                    line = reader.readLine();
                    continue;
                }

                // parse one single line
                if(line.contains("##"))
                {
                    // rule matched
                    rules.add(new RuleElem(line));
                }
                else
                {
                    // black list matched
                    blackLists.add(new BlackElem(line));
                }

                line = reader.readLine();
            }
        } catch (Exception e)
        {
            Log.e("Filter", "Fatal: cannot find or read rules file");
            return false;
        }

        Log.i("Filter", "Filter OK.");
        return true;
    }

    public Pair <QueryStatus, String> query(final String URL)
    {
        // protocol check
        String urlProtocol = URL.substring(0, 5).toLowerCase();
        if(!urlProtocol.equals("https") && !urlProtocol.equals("http:"))
        {
            return new Pair <> (QueryStatus.NO_BLOCK, null);
        }

        // check black list.
        for(final BlackElem each: blackLists)
        {
            if(each.match(URL))
            {
                return new Pair <> (QueryStatus.BLACK, null);
            }
        }

        // check all rules
        Vector<String> scripts = new Vector<>();
        for(RuleElem each: rules)
        {
            if(each.URL.match(URL))
            {
                scripts.add(each.generateScripts());
            }
        }

        // Build up eventual scripts
        if(scripts.isEmpty())
        {
            return new Pair <> (QueryStatus.NO_BLOCK, null);
        }

        StringBuilder script = new StringBuilder(scriptStart);
        for(String each: scripts)
        {
            script.append(each);
        }
        script.append(scriptEnd);

        return new Pair <> (QueryStatus.SCRIPT, script.toString());
    }

    public enum RuleType
    {
        ID, CLASS
    }

    public enum BlackType
    {
        GENERAL, // e.g. info.ruc.edu.cn
        HTTP, // e.g. http://info.ruc.edu.cn
        HTTPS,
        BOTH,
    }

    public enum QueryStatus
    {
        NO_BLOCK,
        SCRIPT,
        BLACK
    }

    public class BlackElem implements Comparable <BlackElem>
    {
        public BlackType type;
        public String protocol;
        public String URL;

        public BlackElem(String line)
        {
            if(line.isEmpty())
            {
                return;
            }

            if(line.substring(0, 7).toLowerCase().equals("http://"))
            {
                type = BlackType.HTTP;
                protocol = "http://";
                URL = line.substring(7);
            }
            else if(line.substring(0, 8).toLowerCase().equals("https://"))
            {
                type = BlackType.HTTPS;
                protocol = "https://";
                URL = line.substring(8);
            }
            else if(line.substring(0, 2).toLowerCase().equals("||"))
            {
                type = BlackType.BOTH;
                protocol = "||";
                URL = line.substring(2);
            }
            else if(isLetter(line.charAt(0)))
            {
                type = BlackType.GENERAL;
                protocol = null;
                URL = line;
            }
            else
            {
                Log.e("Filter", "Cannot parse BlackElem");
            }

            // delete last '/'
            if(URL.charAt(URL.length() - 1) == '/')
                URL = URL.substring(0, URL.length() - 1);
        }

        @Override
        public int compareTo(BlackElem other)
        {
            if(!protocol.equals(other.protocol))
                return protocol.compareTo(other.protocol);
            else if(!URL.equals(other.URL))
                return URL.compareTo(other.URL);
            else
                return type.compareTo(other.type);
        }

        public boolean match(final String url)
        {
            // protocol match test
            if(url.substring(0, 5).equalsIgnoreCase("https"))
            {
                if(type == BlackType.HTTP)
                {
                    return false;
                }
            }
            else
            {
                if(type == BlackType.HTTPS)
                    return false;
            }

            // transform url to a form without the last /
            String urlAfter;
            if(url.charAt(url.length() - 1) == '/')
                urlAfter = url.substring(0, url.length() - 1);
            else
                urlAfter = url;

            // target match test
            switch(type)
            {
                case HTTP:
                case HTTPS:
                case BOTH:
                {
                    return urlAfter.equals(URL);
                }
                case GENERAL:
                {
                    return urlAfter.contains(URL);
                }
            }

            return false;
        }

    }

    public class RuleElem implements Comparable <RuleElem>
    {
        public BlackElem URL;
        public RuleType type;
        public String target;
        public String targetVar;

        public RuleElem(final String line)
        {
            int pos = line.indexOf("##");
            URL = new BlackElem(line.substring(0, pos));

            // TODO: Check is-valid target, e.g. multiple targets ...
            if(line.charAt(pos + 2) == '#')
            {
                type = RuleType.ID;
                target = line.substring(pos + 3);
            }
            else if(line.charAt(pos + 2) == '.')
            {
                type = RuleType.CLASS;
                target = line.substring(pos + 3);
            }
            else
            {
                Log.e("Filter", "Cannot parse RuleElem");
            }
        }

        @Override
        public int compareTo(RuleElem other)
        {
            if(!URL.equals(other.URL))
                return URL.compareTo(other.URL);
            else if(!target.equals(other.target))
                return target.compareTo(other.target);
            else
                return type.compareTo(other.type);
        }

        public String generateScripts()
        {
            generateVarName();

            StringBuilder result = new StringBuilder();
            if(type == RuleType.ID)
            {
                result.append("document.getElementById(\"");
                result.append(target);
                result.append("\").style.display = \"none\";\n");
            }
            else if(type == RuleType.CLASS)
            {
                result.append("var ").append(targetVar).append(" = document.getElementsByClassName(\"")
                        .append(target).append("\");\n").append("for(var ad_blocker_i = 0; ad_blocker_i < ")
                        .append(targetVar).append(".length; ad_blocker_i++){ ")
                        .append(targetVar).append("[ad_blocker_i].style.display = \"none\"; }\n");
            }

            Log.i("Filter", String.format("script generated: %s", result));
            return result.toString();
        }

        public void generateVarName()
        {
            targetVar = transformVar(target) + "_class_search";
        }

        private String transformVar(String ori)
        {
            StringBuilder result = new StringBuilder();
            for(char each: ori.toCharArray())
            {
                if(isLetter(each) || isDigit(each) || each == '_')
                {
                    result.append(each);
                }
                else
                {
                    result.append('_');
                }
            }

            return result.toString();
        }

    }
}
