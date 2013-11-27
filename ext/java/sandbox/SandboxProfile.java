package sandbox;

import org.jruby.Profile;
import org.jruby.runtime.builtin.IRubyObject;
import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;

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
       new String[] {"rubygems", "fileutils","pathname"}
  ));
  private static final Set<String> LoadBlacklist = new HashSet<String>(Arrays.asList(
       new String[] {}
  ));
  private static final Set<String> BuiltinBlacklist = new HashSet<String>(Arrays.asList(
       new String[] {}
  ));

  public boolean allowBuiltin (String name) { System.err.println("allowBuiltin: "+name);return !BuiltinBlacklist.contains(name); }
  public boolean allowClass   (String name) { System.err.println("allowClass: " + name);return !ClassBlacklist.contains(name); }
  public boolean allowModule  (String name) { System.err.println("allowModule: "+ name);return !ModuleBlacklist.contains(name); }
  public boolean allowLoad    (String name) { System.err.println("allowLoad: "  + name);return !LoadBlacklist.contains(name); }
  public boolean allowRequire (String name) { System.err.println("allowRequire: "+name);return !RequireBlacklist.contains(name); }

  // public boolean allowBuiltin (String name) { return true; }
  // public boolean allowClass   (String name) { return true; }
  // public boolean allowModule  (String name) { return true; }
  // public boolean allowLoad    (String name) { return true; }
  // public boolean allowRequire (String name) { return true; }
}
