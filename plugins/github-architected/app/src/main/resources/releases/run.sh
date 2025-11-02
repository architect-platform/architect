#!/usr/bin/env bash
npm init -y
npm install --save-dev semantic-release @semantic-release/changelog @semantic-release/commit-analyzer @semantic-release/release-notes-generator @semantic-release/github @semantic-release/git @semantic-release/exec semantic-release-preconfigured-conventional-commits
touch CHANGELOG.md
npm ci
npx semantic-release