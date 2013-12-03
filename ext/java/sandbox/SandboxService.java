package sandbox;

import java.io.IOException;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.RubyStruct;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.load.BasicLibraryService;

public class SandboxService implements BasicLibraryService {
  public static RubyClass cSandboxFull, cSandboxException, cSandboxResut;

  public boolean basicLoad(Ruby runtime) throws IOException {
    init(runtime);
    return true;
  }

  private void init(Ruby runtime) {
    RubyModule mSandbox = runtime.defineModule("Sandbox");
    mSandbox.defineAnnotatedMethods(SandboxModule.class);

    cSandboxFull = mSandbox.defineClassUnder("Full", runtime.getObject(), FULL_ALLOCATOR);
    cSandboxFull.defineAnnotatedMethods(SandboxFull.class);

    cSandboxResut = RubyStruct.newInstance(runtime.getStructClass(), new IRubyObject[] {
      runtime.newSymbol("result"), runtime.newSymbol("output"), runtime.newSymbol("exception")
    }, org.jruby.runtime.Block.NULL_BLOCK);
    mSandbox.defineConstant("Result", cSandboxResut);

    cSandboxException = mSandbox.defineClassUnder("Exception", runtime.getRuntimeError(), runtime.getRuntimeError().getAllocator());
  }

  protected static final ObjectAllocator FULL_ALLOCATOR = new ObjectAllocator() {
    public IRubyObject allocate(Ruby runtime, RubyClass klass) {
      return new SandboxFull(runtime, klass);
    }
  };
}
