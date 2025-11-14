#!/bin/bash

echo "Building Flipping Optimizer plugin..."
./gradlew shadowJar

if [ $? -eq 0 ]; then
    echo ""
    echo "Build successful! Starting RuneLite..."
    echo ""
    java -ea -jar build/libs/Flip_off-1.0-SNAPSHOT-all.jar
else
    echo ""
    echo "Build failed. Please check the errors above."
    exit 1
fi
