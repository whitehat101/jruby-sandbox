package sandbox;

import org.jruby.Ruby;
import org.jruby.Profile;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.load.LoadService;
import org.jruby.RubyInstanceConfig.LoadServiceCreator;
import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;
import java.util.Iterator;

public class SandboxProfile implements Profile {
  private IRubyObject sandbox;

  public SandboxProfile(IRubyObject sandbox) {
    this.sandbox = sandbox;
  }

  public IRubyObject getSandbox() {
    return sandbox;
  }

  private static final Set<String> ClassBlacklist = new HashSet<String>(Arrays.asList(
       new String[] {"Dir", "File", "File::Stat"}
  ));
  private static final Set<String> ModuleBlacklist = new HashSet<String>(Arrays.asList(
       new String[] {"FileTest"}
  ));
  private static final Set<String> RequireBlacklist = new HashSet<String>(Arrays.asList(
       new String[] {"rubygems", "fileutils", "pathname", "java", "jruby"}
       // "jruby/java/java_ext/java.io", "thread", "thread.jar"
  ));
  private static final Set<String> LoadBlacklist = new HashSet<String>(Arrays.asList(
       new String[] {"jruby/kernel.rb", "jruby/kernel19.rb"}
  ));
  private static final Set<String> BuiltinBlacklist = new HashSet<String>(Arrays.asList(
       new String[] {"java.rb", "jruby.rb"}
       // "java.rb", "jruby.rb", "win32ole.jar"
  ));

  public LoadServiceCreator loadServiceCreator(){
    return new LoadServiceCreator() {
      public LoadService create(Ruby runtime) {
        return new LoadService(runtime) {

          @Override
          public void init(java.util.List additionalDirectories) {
            super.init(additionalDirectories);

            // Add the blacklisted requires to the feature list
            Iterator<String> it = RequireBlacklist.iterator();
            while(it.hasNext())
              addLoadedFeature(it.next());
          }

          @Override
          public void loadFromClassLoader(ClassLoader classLoader, String file, boolean wrap) {
            boolean load = !LoadBlacklist.contains(file);
            System.err.println("loadFromClassLoader: " + load + " " + file);
            if(load)
              super.loadFromClassLoader( classLoader, file, wrap);
          }

        };
      }
    };
  }


  public boolean allowBuiltin (String name) {
    boolean b = !BuiltinBlacklist.contains(name);
    System.err.println("allowBuiltin: " + b + " " + name);
    return b;
  }
  public boolean allowClass   (String name) {
    boolean b = !ClassBlacklist.contains(name);
    System.err.println("  allowClass: " + b + " " + name);
    return b;
  }
  public boolean allowModule  (String name) {
    boolean b = !ModuleBlacklist.contains(name);
    System.err.println(" allowModule: " + b + " " + name);
    return b;
  }
  public boolean allowLoad    (String name) {
    boolean b = !LoadBlacklist.contains(name);
    System.err.println("   allowLoad: " + b + " " + name);
    return b;
  }
  public boolean allowRequire (String name) {
    boolean b = !RequireBlacklist.contains(name);
    System.err.println("allowRequire: " + b + " " + name);
    return b;
  }

  // public boolean allowBuiltin (String name) { return !BuiltinBlacklist.contains(name); }
  // public boolean allowClass   (String name) { return   !ClassBlacklist.contains(name); }
  // public boolean allowModule  (String name) { return  !ModuleBlacklist.contains(name); }
  // public boolean allowLoad    (String name) { return    !LoadBlacklist.contains(name); }
  // public boolean allowRequire (String name) { return !RequireBlacklist.contains(name); }

}
