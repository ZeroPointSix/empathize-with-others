Resumes work from a previous handoff session which are stored in `.claude/handoffs`.

The handoff folder might not exist if there are none.

Requested handoff file: `$ARGUMENTS`

## Process

### 1. Check handoff file

If no handoff file was provided, list them all.  Eg:


```
echo "## Available Handoffs"
echo ""
for file in .claude/handoffs/*.md; do
  if [ -f "$file" ]; then
    title=$(grep -m 1 "^# " "$file" | sed 's/^# //')
    basename=$(basename "$file")
    echo "* \`$basename\`: $title"
  fi
done
echo ""
echo "To pickup a handoff, use: /pickup <filename>"
```

### 2. List handoff file

If a handoff file was provided locate it in `.claude/handoffs` and read it.  Note that this file might be misspelled or the user might have only partially listed it.  If there are multiple matches, ask the user which one they want to continue with.  The file contains the instructions for how you should continue.