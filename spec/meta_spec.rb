require 'rspec'
require 'sandbox'

describe Sandbox do

  class Bar
    attr_reader :name
    def initialize(name)
      @name = name
    end
  end

  describe ".safe" do
    subject { Sandbox.safe}

    it 'should allow meta programming' do
      subject.activate!
      code =<<EORUBY
      class Array
        def second
          self[1]
        end
      end
      [5,10].second
EORUBY
      result = subject.eval(code)
      expect(result).to eq(10)
    end

    it "should allow some class eval" do
      foo = Bar.new("bar")
      Bar.class_eval do 
        def bazz
          name.upcase
        end
      end
      foo.bazz.should eq("BAR")
    end

    it "should allow instance eval" do
      foo = Bar.new("bar")
      foo.instance_eval do 
        def bazz
          name.upcase
        end
      end
      foo.bazz.should eq("BAR")
    end

  end
end
