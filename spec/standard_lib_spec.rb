require 'rspec'
require 'sandbox'

describe Sandbox do

  describe ".safe" do
    subject { Sandbox.safe}

    it 'should load json from standard lib' do
      pending "I would like to load standard lib, but not doing that yet"
      subject.activate!
      code =<<EORUBY
      require 'json'
      hash = {oh: "hai"}
      as_json = hash.to_json
      JSON.parse(as_json)["oh"]
EORUBY
      result = subject.eval(code)
      expect(result).to eq("hai")
    end

  end
end
