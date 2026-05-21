#!/bin/bash
# 30秒ごとにワークフロー実行状況を確認（5分間）
TIMEOUT=$((5 * 60))
START=$(date +%s)

echo "GitHub Actions ワークフロー実行確認"
echo "ブラウザで以下を開いてください："
echo "https://github.com/kondoshinichiro0517/shopping-list-app/actions"
echo ""

while true; do
    CURRENT=$(date +%s)
    ELAPSED=$((CURRENT - START))
    
    if [ $ELAPSED -gt $TIMEOUT ]; then
        echo "タイムアウト（5分経過）"
        break
    fi
    
    echo "[$(date +'%H:%M:%S')] ワークフロー実行中...（残り時間: $((TIMEOUT - ELAPSED))秒）"
    sleep 30
done
