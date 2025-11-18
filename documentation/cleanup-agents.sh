#!/bin/bash

# Script to cleanup Cursor agents
# Usage: ./cleanup-agents.sh <api_token>

set -euo pipefail

# Check if jq is installed
if ! command -v jq &> /dev/null; then
    echo "Error: jq is required but not installed."
    echo "Install it with: brew install jq (macOS) or apt-get install jq (Linux)"
    exit 1
fi

# Check if API token is provided
if [ $# -eq 0 ]; then
    echo "Error: API token is required"
    echo "Usage: $0 <api_token>"
    exit 1
fi

API_TOKEN="$1"
API_BASE_URL="https://api.cursor.com/v0"
API_CALL_COUNT=0
RATE_LIMIT_THRESHOLD=20
RATE_LIMIT_PAUSE=40

# Function to check rate limit and pause if needed
check_rate_limit() {
    ((API_CALL_COUNT++))
    if [ $((API_CALL_COUNT % RATE_LIMIT_THRESHOLD)) -eq 0 ]; then
        echo ""
        echo "⚠️  Rate limit protection: Pausing for ${RATE_LIMIT_PAUSE} seconds after ${API_CALL_COUNT} API calls..."
        sleep ${RATE_LIMIT_PAUSE}
        echo "✓ Resuming operations..."
        echo ""
    fi
}

echo "Fetching agents list..."
# Fetch agents list
RESPONSE=$(curl -s -X 'GET' \
  "${API_BASE_URL}/agents?limit=100" \
  -H 'accept: application/json' \
  -H "Authorization: Bearer ${API_TOKEN}")

# Count the initial GET request
check_rate_limit

# Check if response is valid JSON and contains agents
if ! echo "$RESPONSE" | jq -e '.agents' > /dev/null 2>&1; then
    echo "Error: Invalid response from API"
    echo "$RESPONSE"
    exit 1
fi

# Extract agent IDs from the response
AGENT_IDS=$(echo "$RESPONSE" | jq -r '.agents[]?.id // empty')

if [ -z "$AGENT_IDS" ]; then
    echo "No agents found to delete."
    exit 0
fi

# Count agents
AGENT_COUNT=$(echo "$AGENT_IDS" | wc -l | tr -d ' ')
echo "Found $AGENT_COUNT agent(s) to delete."

# Confirm deletion
read -p "Do you want to delete all $AGENT_COUNT agent(s)? (yes/no): " CONFIRM
if [ "$CONFIRM" != "yes" ]; then
    echo "Deletion cancelled."
    exit 0
fi

# Delete each agent
DELETED=0
FAILED=0

while IFS= read -r AGENT_ID; do
    if [ -z "$AGENT_ID" ]; then
        continue
    fi

    echo "Deleting agent: $AGENT_ID"

    DELETE_RESPONSE=$(curl -s -w "\n%{http_code}" -X 'DELETE' \
      "${API_BASE_URL}/agents/${AGENT_ID}" \
      -H 'accept: application/json' \
      -H "Authorization: Bearer ${API_TOKEN}")

    HTTP_CODE=$(echo "$DELETE_RESPONSE" | tail -n1)
    BODY=$(echo "$DELETE_RESPONSE" | sed '$d')

    if [ "$HTTP_CODE" -ge 200 ] && [ "$HTTP_CODE" -lt 300 ]; then
        echo "  ✓ Successfully deleted agent: $AGENT_ID"
        ((DELETED++))
    else
        echo "  ✗ Failed to delete agent: $AGENT_ID (HTTP $HTTP_CODE)"
        echo "    Response: $BODY"
        ((FAILED++))
    fi

    # Check rate limit after each DELETE call
    check_rate_limit
done <<< "$AGENT_IDS"

echo ""
echo "Summary:"
echo "  Deleted: $DELETED"
echo "  Failed:  $FAILED"
echo "  Total:   $AGENT_COUNT"

