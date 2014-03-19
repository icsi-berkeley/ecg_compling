import java.io.File;
import java.io.IOException;
import java.util.Map.Entry;

import org.ini4j.Ini;
import org.ini4j.Profile.Section;

public class Tests {

	static void sample05(File file) throws IOException
   {
       Ini ini = new Ini(file);
       
       ini.setComment("This is just an ordinary comment");

       ini.load();
       System.out.println(ini);
       
       // lets add a section, it will create needed intermediate sections as well
       ini.add("root/child/sub");

       //
       Ini.Section root;
       Ini.Section sec;

       root = ini.get("root");
       sec = root.getChild("child").getChild("sub");
       
       System.out.println(sec);
       
       sec.addChild("cazzo").add("merda", "del Culo");

       // or...
       sec = root.lookup("child", "sub");
       
       System.out.println(sec);
       
       sec.add("cacca", "culo");

       // or...
       sec = root.lookup("child/sub");
       System.out.println(sec);
       sec.add("dio", "porcone");

       // or even...
       sec = ini.get("root/child/sub");
       System.out.println(sec);
       
//       System.out.println(ini);
 
       for (int i = 0; i < 100; ++i)
			root.add("sentence", String.format("This is the sentence number %d .", i), i);
       
//       System.out.println(ini);
      
       System.out.printf("44: %s\n", root.get("sentence", 44));
       System.out.printf("144: %s\n", root.get("sentence", 144));
       
       for (int i = 0; i < 100; ++i)
      	 root.add(String.format("sentence_%d", i), String.format("sent_#_%d", i));
       
//       System.out.println(ini);
       
       ini.store();
   }

	static void sample06(File file) throws IOException {
      Ini ini = new Ini(file);
      ini.load();
    
      for (Entry<String, Section> e : ini.entrySet()) 
      	System.out.printf("%s: %s\n", e.getKey(), e.getValue());
	}
	
	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		//sample05(new File(args[1]));
		sample06(new File(args[0]));
	}
}
