require 'fakefs/safe'

# unfortunately, the authors of FakeFS used `extend self` in FileUtils, instead of `module_function`.
# I fixed it for them
(FakeFS::FileUtils.methods - Module.methods - Kernel.methods).each do |module_method_name|
  FakeFS::FileUtils.send(:module_function, module_method_name)
end

# I think this was to fix a bug in RSpec
FakeFS::File::FNM_SYSCASE = 0
