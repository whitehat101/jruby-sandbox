require 'rspec'
require 'sandbox'

describe Sandbox do

  describe ".safe" do
    subject { Sandbox.safe }

    it 'should capture output' do
      subject.activate!
      subject.eval('puts "yo"')
      expect(subject.getStdOut).to match(/yo/)
    end

    it 'should capture output' do
      subject.activate!
      result = subject.eval_with_result('print "yo"; 5+5')
      expect(result.output).to match(/yo/)
    end
    
    it 'should capture return' do
      subject.activate!
      result = subject.eval_with_result('puts "yo"; 5+5')
      result.result.should eq(10)
    end
  end
end
