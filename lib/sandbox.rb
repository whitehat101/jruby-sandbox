require 'sandbox/version'
require 'sandbox/sandbox'
require 'sandbox/fakefs'

module Sandbox

  def self.new opts={}
    Full.new opts
  end

  def self.safe opts={}
    Safe.new opts
  end

  class Full
    # Result = Struct.new(:output, :result, :exception)
    def eval_with_result(code)
      result = eval_with_timeout(code)
      Result.new(result,getStdOut)
    end

    def eval_with_timeout(code, seconds = 10)
      sandboxResult, sandboxException = nil

      thread = Thread.start(code) do
        begin
          sandboxResult = eval code
        rescue Exception => sandboxException
        end
      end

      thread.join seconds

      if thread.alive?
        thread.respond_to?(:kill!) ? thread.kill! : thread.kill
        raise Sandbox::Exception, "TimeoutError: #{self.class} timed out"
      end

      raise sandboxException if sandboxException
      sandboxResult
    end

  end

  class Safe < Full
    def activate!
      activate_fakefs

      keep_singleton_methods(:Kernel, KERNEL_S_METHODS)
      keep_singleton_methods(:Symbol, SYMBOL_S_METHODS)
      keep_singleton_methods(:String, STRING_S_METHODS)
      keep_singleton_methods(:IO, IO_S_METHODS)

      keep_methods(:Kernel, KERNEL_METHODS)
      keep_methods(:NilClass, NILCLASS_METHODS)
      keep_methods(:Symbol, SYMBOL_METHODS)
      keep_methods(:TrueClass, TRUECLASS_METHODS)
      keep_methods(:FalseClass, FALSECLASS_METHODS)
      keep_methods(:Enumerable, ENUMERABLE_METHODS)
      keep_methods(:String, STRING_METHODS)
    end

    def activate_fakefs
      # Bring the FakeFS into the sandbox
      import  FakeFS
      ref     FakeFS::Dir
      ref     FakeFS::File
      ref     FakeFS::FileTest
      ref     FakeFS::Pathname
      import  FakeFS::FileUtils #import FileUtils because it is a module

      eval <<-RUBY
        Object.class_eval do
          const_set(:Dir,       FakeFS::Dir)
          const_set(:File,      FakeFS::File)
          const_set(:FileUtils, FakeFS::FileUtils)
          const_set(:Pathname,  FakeFS::Pathname)
          const_set(:FileTest,  FakeFS::FileTest)
        end
      RUBY

      FakeFS::FileSystem.clear
    end


    IO_S_METHODS = %w[
      new
      foreach
      open
    ]

    KERNEL_S_METHODS = %w[
      Array
      binding
      block_given?
      catch
      chomp
      chomp!
      chop
      chop!
      eval
      fail
      Float
      format
      global_variables
      gsub
      gsub!
      Integer
      iterator?
      lambda
      local_variables
      loop
      method_missing
      proc
      raise
      scan
      split
      sprintf
      String
      sub
      sub!
      throw
    ].freeze

    SYMBOL_S_METHODS = %w[
      all_symbols
    ].freeze

    STRING_S_METHODS = %w[
      new
    ].freeze

    KERNEL_METHODS = %w[
      ==
      ===
      =~
      !~
      Array
      binding
      block_given?
      catch
      chomp
      chomp!
      chop
      chop!
      class
      clone
      dup
      eql?
      equal?
      eval
      fail
      Float
      format
      freeze
      frozen?
      global_variables
      gsub
      gsub!
      hash
      id
      initialize_copy
      inspect
      instance_eval
      instance_of?
      instance_variables
      instance_variable_get
      instance_variable_set
      instance_variable_defined?
      Integer
      is_a?
      iterator?
      kind_of?
      lambda
      local_variables
      loop
      methods
      method_missing
      nil?
      private_methods
      print
      proc
      protected_methods
      public_methods
      puts
      raise
      remove_instance_variable
      respond_to?
      respond_to_missing?
      scan
      send
      singleton_methods
      singleton_method_added
      singleton_method_removed
      singleton_method_undefined
      split
      sprintf
      String
      sub
      sub!
      taint
      tainted?
      throw
      to_a
      to_s
      type
      untaint
      __send__
    ].freeze

    NILCLASS_METHODS = %w[
      &
      inspect
      nil?
      to_a
      to_f
      to_i
      to_s
      ^
      |
    ].freeze

    SYMBOL_METHODS = %w[
      ===
      id2name
      inspect
      to_i
      to_int
      to_s
      to_sym
    ].freeze

    TRUECLASS_METHODS = %w[
      &
      to_s
      ^
      |
    ].freeze

    FALSECLASS_METHODS = %w[
      &
      to_s
      ^
      |
    ].freeze

    ENUMERABLE_METHODS = %w[
      all?
      any?
      collect
      detect
      each_with_index
      entries
      find
      find_all
      grep
      include?
      inject
      map
      max
      member?
      min
      partition
      reject
      select
      sort
      sort_by
      to_a
      zip
    ].freeze

    STRING_METHODS = %w[
      %
      *
      +
      <<
      <=>
      ==
      =~
      capitalize
      capitalize!
      casecmp
      center
      chomp
      chomp!
      chop
      chop!
      concat
      count
      crypt
      delete
      delete!
      downcase
      downcase!
      dump
      each
      each_byte
      each_line
      empty?
      eql?
      gsub
      gsub!
      hash
      hex
      include?
      index
      initialize
      initialize_copy
      insert
      inspect
      intern
      length
      ljust
      lines
      lstrip
      lstrip!
      match
      next
      next!
      oct
      replace
      reverse
      reverse!
      rindex
      rjust
      rstrip
      rstrip!
      scan
      size
      slice
      slice!
      split
      squeeze
      squeeze!
      strip
      strip!
      start_with?
      sub
      sub!
      succ
      succ!
      sum
      swapcase
      swapcase!
      to_f
      to_i
      to_s
      to_str
      to_sym
      tr
      tr!
      tr_s
      tr_s!
      upcase
      upcase!
      upto
      []
      []=
    ].freeze
  end
end
