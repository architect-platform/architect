# typed: false
# frozen_string_literal: true

class Architect < Formula
  desc "Plugin-based task execution framework for developer workflows"
  homepage "https://github.com/architect-platform/architect"
  version "0.0.0"  # Updated automatically by update-homebrew workflow

  on_macos do
    if Hardware::CPU.arm?
      url "https://github.com/architect-platform/architect/releases/download/v#{version}/architect-macos-arm64"
      sha256 "0000000000000000000000000000000000000000000000000000000000000000"  # Updated automatically

      def install
        bin.install "architect-macos-arm64" => "architect"
      end
    else
      url "https://github.com/architect-platform/architect/releases/download/v#{version}/architect-macos-x86_64"
      sha256 "0000000000000000000000000000000000000000000000000000000000000000"  # Updated automatically

      def install
        bin.install "architect-macos-x86_64" => "architect"
      end
    end
  end

  on_linux do
    if Hardware::CPU.arm?
      url "https://github.com/architect-platform/architect/releases/download/v#{version}/architect-linux-arm64"
      sha256 "0000000000000000000000000000000000000000000000000000000000000000"  # Updated automatically

      def install
        bin.install "architect-linux-arm64" => "architect"
      end
    else
      url "https://github.com/architect-platform/architect/releases/download/v#{version}/architect-linux-x86_64"
      sha256 "0000000000000000000000000000000000000000000000000000000000000000"  # Updated automatically

      def install
        bin.install "architect-linux-x86_64" => "architect"
      end
    end
  end

  def post_install
    # Generate shell completions
    (bash_completion/"architect").write `#{bin}/architect completion bash`
    (zsh_completion/"_architect").write `#{bin}/architect completion zsh`
    (fish_completion/"architect.fish").write `#{bin}/architect completion fish`
  rescue StandardError
    # Completion generation is optional — don't fail install
    nil
  end

  test do
    assert_match "architect", shell_output("#{bin}/architect --version")
  end
end
