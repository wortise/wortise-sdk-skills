#!/bin/bash
# Copies the fixture app into the run workspace.
set -e
cp -R "$(dirname "$0")/app" ./app
