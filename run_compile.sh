#!/bin/bash
cd /Users/wade/IdeaProjects/RuoYi-Vue-Plus/aileme
mvn compile -pl ai-le-me-modules/ai-le-me-shejiao-app -am -o -q 2>&1 | grep -E "error:|incompatible|cannot find|required:" | head -200
echo "===DONE==="
