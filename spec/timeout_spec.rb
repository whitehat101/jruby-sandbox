require 'rspec'
require 'sandbox'

describe Sandbox do

  describe ".new" do
    subject { Sandbox.new}

    it 'should timeout' do
      expect {
        subject.eval_with_timeout('sleep(0.25)', 0.2)
      }.to raise_error(Sandbox::SandboxException, /Timeout::Error/)
    end
  end
end
