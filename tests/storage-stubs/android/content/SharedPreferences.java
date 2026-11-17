package android.content;
public final class SharedPreferences {
 private final java.util.Map<String,String>values=new java.util.HashMap<>();
 public String getString(String k,String fallback){return values.containsKey(k)?values.get(k):fallback;}
 public Editor edit(){return new Editor();}
 public final class Editor {public Editor putString(String k,String value){values.put(k,value);return this;}public void apply(){}}
}
