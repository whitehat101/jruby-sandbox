package sandbox;

import java.util.Collection;
import java.util.Map;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.io.IOException;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyHash;
import org.jruby.RubyKernel;
import org.jruby.RubyModule;
import org.jruby.RubyObject;
import org.jruby.RubyString;
import org.jruby.RubyStruct;
import org.jruby.RubySymbol;
import org.jruby.RubyInstanceConfig;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.exceptions.RaiseException;
import org.jruby.common.IRubyWarnings;
import org.jruby.CompatVersion;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.internal.runtime.methods.DynamicMethod;

@JRubyClass(name="Sandbox::Full")
public class SandboxFull extends RubyObject {
  private Ruby runtime, sandbox;
  private ByteArrayOutputStream stdOut, lastOutput;

  // Ruby Object Cache
  private RubyClass sandboxResultClass, sandboxExceptionClass;
  private RubySymbol loadPathSym;

  public SandboxFull(Ruby _runtime, RubyClass type) {
    super(_runtime, type);
    runtime = _runtime;

    // Output Buffers
    stdOut = new ByteArrayOutputStream();
    lastOutput = new ByteArrayOutputStream();

    // Object Cache
    sandboxResultClass = (RubyClass) runtime.getClassFromPath("Sandbox::Result");
    sandboxExceptionClass = (RubyClass) runtime.getClassFromPath("Sandbox::Exception");
    loadPathSym = runtime.newSymbol("LOAD_PATH");

    reload();
  }

  // @JRubyMethod(required=0)
  // public IRubyObject initialize(ThreadContext context) {
  //   System.err.println("SandboxFull#new (no args)");
  //   return context.nil;
  // }

  @JRubyMethod
  public IRubyObject initialize(ThreadContext context, IRubyObject value) {
    RubyHash opts = value.convertToHash();
    // System.err.println("SandboxFull#new (arg) = " + opts.to_s(context).asJavaString());
    // System.err.println("SandboxFull#new (arg) = " + opts.keySet().toString());

    // if(opts.containsKey(context.runtime.newSymbol("LOAD_PATH")))
    if(opts.containsKey(loadPathSym)){
      opts.get(loadPathSym);
      System.err.println("SandboxFull#new (arg) = LOAD_PATH");
    }
    return context.nil;
  }

  @JRubyMethod
  public IRubyObject reload() {
    RubyInstanceConfig externalCfg = runtime.getInstanceConfig();
    RubyInstanceConfig cfg = new RubyInstanceConfig();
    SandboxProfile profile = new SandboxProfile(this);
    stdOut.reset();
    lastOutput.reset();

    cfg.setInput(externalCfg.getInput());
    cfg.setError(externalCfg.getError());
    cfg.setOutput(new PrintStream(lastOutput));
    cfg.setObjectSpaceEnabled(externalCfg.isObjectSpaceEnabled());
    cfg.setCompatVersion(CompatVersion.RUBY1_9);
    cfg.setScriptFileName("(sandbox)");
    cfg.setBacktraceMask(true);
    cfg.setProfile(profile);
    cfg.setLoadServiceCreator(profile.loadServiceCreator());
    // cfg.setEnvironment(java.util.Map newEnvironment);
    // cfg.setKCode(org.jruby.util.KCode.UTF8); // doesn't affect __ENCODING__
    // cfg.setLoadPaths(java.util.List<java.lang.String> loadPaths);

    sandbox = Ruby.newInstance(cfg);
    profile.postBootCleanup(sandbox);

    sandbox.defineClass("BoxedClass", sandbox.getObject(), sandbox.getObject().getAllocator())
           .defineAnnotatedMethods(BoxedClass.class);

    return this;
  }

  @JRubyMethod(required=1)
  public IRubyObject eval(IRubyObject str) {
    lastOutput.reset();
    try {
      IRubyObject result = sandbox.evalScriptlet("#encoding: utf-8\n"+str.asJavaString(), sandbox.getCurrentContext().getCurrentScope());
      IRubyObject unboxedResult = unbox(result);
      return unboxedResult;
    } catch (RaiseException e) {
      String msg = e.getException().callMethod(sandbox.getCurrentContext(), "message").asJavaString();
      String path = e.getException().type().getName();
      throw new RaiseException( runtime, sandboxExceptionClass, path + ": " + msg, false );
    } catch (Exception e) {
      System.err.println("NativeException: " + e);
      e.printStackTrace();
      runtime.getWarnings().warn(IRubyWarnings.ID.MISCELLANEOUS, "NativeException: " + e);
      return runtime.getNil();
    } finally {
      try { lastOutput.writeTo(stdOut); } catch (IOException e) {
        // I can't imagine why this would actually happen
        System.err.println("IOException while copying lastOutput to stdOut");
      }
    }
  }

  @JRubyMethod(required=1)
  public IRubyObject exec(IRubyObject str) {
    RubyStruct resultStruct = RubyStruct.newStruct(sandboxResultClass, Block.NULL_BLOCK);

    try {
      // Set :result
      resultStruct.set(eval(str), 0);
    } catch (RaiseException e) {
      // Set :exception
      String message = e.getException().message(runtime.getCurrentContext()).asJavaString();
      resultStruct.set(runtime.newString(message), 2);
    }
    // Set :output
    resultStruct.set(getLastOut(), 1);
    return resultStruct;
  }

  @JRubyMethod
  public RubyString getStdOut() {
    return runtime.newString(stdOut.toString());
  }

  @JRubyMethod
  public RubyString getLastOut() {
    return runtime.newString(lastOutput.toString());
  }

  @JRubyMethod(name="import", required=1)
  public IRubyObject _import(IRubyObject klass) {
    if (!(klass instanceof RubyModule)) {
      throw runtime.newTypeError(klass, runtime.getClass("Module"));
    }
    String name = ((RubyModule) klass).getName();
    importClassPath(name, false);
    return runtime.getNil();
  }

  @JRubyMethod(required=1)
  public IRubyObject ref(IRubyObject klass) {
    if (!(klass instanceof RubyModule)) {
      throw runtime.newTypeError(klass, runtime.getClass("Module"));
    }
    String name = ((RubyModule) klass).getName();
    importClassPath(name, true);
    return runtime.getNil();
  }

  private RubyModule importClassPath(String path, final boolean link) {
    RubyModule runtimeModule = runtime.getObject();
    RubyModule wrappedModule = sandbox.getObject();

    if (path.startsWith("#")) {
      throw runtime.newArgumentError("can't import anonymous class " + path);
    }

    for (String name : path.split("::")) {
      runtimeModule = (RubyModule) runtimeModule.getConstantAt(name);
      // Create the module when it did not exist yet...
      if (wrappedModule.const_defined_p(sandbox.getCurrentContext(), sandbox.newString(name)).isFalse()) {
        // The BoxedClass takes the place of Object as top of the inheritance
        // hierarchy. As a result, we can intercept all new instances that are
        // created and all method_missing calls.
        RubyModule sup = sandbox.getClass("BoxedClass");
        if (!link && runtimeModule instanceof RubyClass) {
          // If we're importing a class, recursively import all of its
          // superclasses as well.
          sup = importClassPath(runtimeModule.getSuperClass().getName(), true);
        }

        RubyClass klass = (RubyClass) sup;
        if (wrappedModule == sandbox.getObject()) {

          if (link || runtimeModule instanceof RubyClass){ // if this is a ref and not an import
            wrappedModule = sandbox.defineClass(name, klass, klass.getAllocator());
          } else {
            wrappedModule = sandbox.defineModule(name);
          }

        } else {
          if (runtimeModule instanceof RubyClass){
            wrappedModule = wrappedModule.defineClassUnder(name, klass, klass.getAllocator());
          } else {
            wrappedModule = wrappedModule.defineModuleUnder(name);
          }

        }
      } else {
        // ...or just resolve it, if it was already known
        wrappedModule = (RubyModule) wrappedModule.getConstantAt(name);
      }

      // Check the consistency of the hierarchy
      if (runtimeModule instanceof RubyClass) {
        if (!link && !runtimeModule.getSuperClass().getName().equals(wrappedModule.getSuperClass().getName())) {
          throw runtime.newTypeError("superclass mismatch for class " + runtimeModule.getSuperClass().getName());
        }
      }

      if (link || runtimeModule instanceof RubyClass) {
        linkObject(runtimeModule, wrappedModule);
      } else {
        copyMethods(runtimeModule, wrappedModule);
      }
    }

    return runtimeModule;
  }

  private void copyMethods(RubyModule from, RubyModule to) {
    to.getMethodsForWrite().putAll(from.getMethods());
    to.getSingletonClass().getMethodsForWrite().putAll(from.getSingletonClass().getMethods());
  }

  @JRubyMethod(required=2)
  public IRubyObject keep_methods(IRubyObject className, IRubyObject methods) {
    RubyModule module = sandbox.getModule(className.asJavaString());
    if (module != null) {
      keepMethods(module, methods.convertToArray());
    }
    return methods;
  }

  @JRubyMethod(required=2)
  public IRubyObject keep_singleton_methods(IRubyObject className, IRubyObject methods) {
    RubyModule module = sandbox.getModule(className.asJavaString()).getSingletonClass();
    if (module != null) {
      keepMethods(module, methods.convertToArray());
    }
    return methods;
  }

  private void keepMethods(RubyModule module, Collection retain) {
    for (Map.Entry<String, DynamicMethod> methodEntry : module.getMethods().entrySet()) {
      String methodName = methodEntry.getKey();
      if (!retain.contains(methodName)) {
        removeMethod(module, methodName);
      }
    }
  }

  @JRubyMethod(required=2)
  public IRubyObject remove_method(IRubyObject className, IRubyObject methodName) {
    RubyModule module = sandbox.getModule(className.asJavaString());
    if (module != null) {
      removeMethod(module, methodName.asJavaString());
    }
    return runtime.getNil();
  }

  @JRubyMethod(required=2)
  public IRubyObject remove_singleton_method(IRubyObject className, IRubyObject methodName) {
    RubyModule module = sandbox.getModule(className.asJavaString()).getSingletonClass();
    if (module != null) {
      removeMethod(module, methodName.asJavaString());
    }
    return runtime.getNil();
  }

  private void removeMethod(RubyModule module, String methodName) {
    // System.err.println("removing method " + methodName + " from " + module.inspect().asJavaString());
    module.removeMethod(sandbox.getCurrentContext(), methodName);
  }

  @JRubyMethod(required=1)
  public IRubyObject load(IRubyObject str) {
    try {
      sandbox.getLoadService().load(str.asJavaString(), true);
      return runtime.getTrue();
    } catch (RaiseException e) {
      e.printStackTrace();
      return runtime.getFalse();
    }
  }

  @JRubyMethod(required=1)
  public IRubyObject require(IRubyObject str) {
    try {
      IRubyObject result = RubyKernel.require(sandbox.getKernel(), sandbox.newString(str.asJavaString()), Block.NULL_BLOCK);
      return unbox(result);
    } catch (RaiseException e) {
      e.printStackTrace();
      return runtime.getFalse();
    }
  }

  private IRubyObject unbox(IRubyObject obj) {
    return box(obj);
  }

  private IRubyObject rebox(IRubyObject obj) {
    return box(obj);
  }

  private IRubyObject box(IRubyObject obj) {
    if (obj.isImmediate()) {
      return cross(obj);
    } else {
      // If this object already existed and was returned from the sandbox
      // runtime on an earlier occasion, it will already contain a link to its
      // brother in the regular runtime and we can safely return that link.
      IRubyObject link = getLinkedObject(obj);
      if (!link.isNil()) {
        IRubyObject box = getLinkedBox(obj);
        if (box == this) return link;
      }

      // Is the class already known on both sides of the fence?
      IRubyObject klass = constFind(obj.getMetaClass().getRealClass().getName());
      link = runtime.getNil();
      if (!klass.isNil()) {
        link = getLinkedObject(klass);
      }

      if (link.isNil()) {
        return cross(obj);
      } else {
        IRubyObject v = ((RubyClass)klass).allocate();
        linkObject(obj, v);
        return v;
      }
    }
  }

  private IRubyObject cross(IRubyObject obj) {
    IRubyObject dumped = sandbox.getModule("Marshal").callMethod(sandbox.getCurrentContext(), "dump", obj);
    return runtime.getModule("Marshal").callMethod(runtime.getCurrentContext(), "load", dumped);
  }

  protected static IRubyObject getLinkedObject(IRubyObject arg) {
    IRubyObject object = arg.getRuntime().getNil();
    if (arg.getInstanceVariables().getInstanceVariable("__link__") != null) {
      object = (IRubyObject) arg.getInstanceVariables().fastGetInstanceVariable("__link__");
    }
    return object;
  }

  protected static IRubyObject getLinkedBox(IRubyObject arg) {
    IRubyObject object = arg.getRuntime().getNil();
    if (arg.getInstanceVariables().getInstanceVariable("__box__") != null) {
      object = (IRubyObject) arg.getInstanceVariables().fastGetInstanceVariable("__box__");
    }
    return object;
  }

  // protected static RaiseException rubyException(IRubyObject recv, String klass, String message) {
  //   Ruby runtime = recv.getRuntime();
  //   return new RaiseException( runtime, (RubyClass) runtime.getClassFromPath(klass), message, false );
  // }

  private void linkObject(IRubyObject runtimeObject, IRubyObject wrappedObject) {
    wrappedObject.getInstanceVariables().setInstanceVariable("__link__", runtimeObject);
    wrappedObject.getInstanceVariables().setInstanceVariable("__box__", this);
  }

  private IRubyObject constFind(String path) {
    try {
      return sandbox.getClassFromPath(path);
    } catch (Exception e) {
      return sandbox.getNil();
    }
  }

  protected IRubyObject runMethod(IRubyObject recv, String name, IRubyObject[] args, Block block) {
    IRubyObject[] args2 = new IRubyObject[args.length];
    for (int i = 0; i < args.length; i++) {
      args2[i] = unbox(args[i]);
    }
    IRubyObject recv2 = unbox(recv);
    IRubyObject result = recv2.callMethod(runtime.getCurrentContext(), name, args2, block);
    return rebox(result);
  }
}
