require 'bundler'
Bundler::GemHelper.install_tasks

require 'rake/javaextensiontask'

Rake::JavaExtensionTask.new('sandbox') do |ext|
  jruby_home = RbConfig::CONFIG['prefix']
  # -Xlint:deprecation
  ext.ext_dir = 'ext/java'
  ext.lib_dir = 'lib/sandbox'
  jars = ["#{jruby_home}/lib/jruby.jar"] + FileList['lib/*.jar']
  ext.classpath = jars.map { |x| File.expand_path(x) }.join(':')
end

require 'rspec/core/rake_task'

RSpec::Core::RakeTask.new(:spec) do |t|
  t.pattern = 'spec/**/*_spec.rb'
  t.rspec_opts = ['--backtrace']
end

# Make sure the jar is up to date before running specs
task :spec => :compile

task :default => :spec
