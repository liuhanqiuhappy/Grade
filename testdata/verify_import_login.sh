#!/usr/bin/env bash
# 验证链路：建班 -> 批量导入学生 -> 学生登录 -> 强制改密拦截 -> 改密 -> 重新登录 -> 混合文件容错
# 前置：后端已启动（http://localhost:8080），数据库为清理后的干净状态
# 用法：bash verify_import_login.sh
#       如 teacher01 口令已改过：TEACHER_PWD='你的口令' bash verify_import_login.sh

set -u
export PYTHONIOENCODING=utf-8   # Windows 下避免中文 JSON 打印报 UnicodeEncodeError
BASE="http://localhost:8080"
DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$DIR"

TEACHER_USER="${TEACHER_USER:-teacher01}"
TEACHER_PWD="${TEACHER_PWD:-pass1234}"
CLASS_NAME="${CLASS_NAME:-软件工程2301班}"
STU_NO="${STU_NO:-20270201}"
STU_INIT_PWD="pass1234"          # 代码常量 DEFAULT_STUDENT_PASSWORD
STU_NEW_PWD="Grade@2026"

TJ="$DIR/.cookies_teacher.txt"
SJ="$DIR/.cookies_student.txt"
rm -f "$TJ" "$SJ"

say() { printf '\n\033[1m==== %s ====\033[0m\n' "$*"; }
# 所有请求都必须绕过代理，否则本机 8080 会被 HTTP_PROXY 劫持
TC() { curl -s --noproxy '*' -b "$TJ" -c "$TJ" "$@"; }
SC() { curl -s --noproxy '*' -b "$SJ" -c "$SJ" "$@"; }
CT='Content-Type: application/json;charset=UTF-8'

show() {
python -c "
import sys, json
raw = sys.stdin.read()
try:
    d = json.loads(raw)
except Exception:
    print('  [非 JSON 响应]', raw[:300]); sys.exit(0)
print('  code=%s  message=%s' % (d.get('code'), d.get('message')))
data = d.get('data')
if isinstance(data, dict):
    for k, v in data.items():
        if isinstance(v, list):
            print('  %s: %d 条' % (k, len(v)))
            for it in v[:12]:
                print('     ', it)
        else:
            print('  %s = %s' % (k, v))
elif isinstance(data, list):
    print('  共 %d 条' % len(data))
    for it in data[:12]:
        print('     ', it)
"

}

say "0. 探活"
PING=$(TC "$BASE/api/ping")
echo "$PING" | show
if ! echo "$PING" | grep -q '"code"[[:space:]]*:[[:space:]]*0'; then
  echo "  ✗ 后端不可达。请先启动后端再重跑本脚本（启动命令见 验证清单.md）。"
  exit 1
fi

say "1. 教师登录（$TEACHER_USER）"
TC -X POST "$BASE/api/identity/login" -H "$CT" \
   -d "{\"username\":\"$TEACHER_USER\",\"password\":\"$TEACHER_PWD\"}" | show

say "2. 新建班级「$CLASS_NAME」"
CREATE=$(TC -X POST "$BASE/api/classes" -H "$CT" -d "{\"name\":\"$CLASS_NAME\"}")
echo "$CREATE" | show
CLASS_ID=$(echo "$CREATE" | python -c "import sys,json;d=json.load(sys.stdin);v=d.get('data');print(v.get('id') if isinstance(v,dict) else '')" 2>/dev/null)
if [ -z "$CLASS_ID" ]; then
  say "2b. 同名班级已存在，改为复用（无需重复建班）"
  CLASS_ID=$(TC "$BASE/api/classes?page=1&size=100&keyword=$CLASS_NAME" | python -c "
import sys,json
d=json.load(sys.stdin); v=d.get('data') or {}
rows=v.get('records') or v.get('list') or (v if isinstance(v,list) else [])
print(rows[0]['id'] if rows else '')" 2>/dev/null)
fi
echo "  -> 使用 classId = ${CLASS_ID:-未取到}"
if [ -z "$CLASS_ID" ]; then echo "  建班/取班失败，后续步骤终止"; exit 1; fi

say "3. 批量导入 import_valid.xlsx（12 行合法数据）"
TC -X POST "$BASE/api/classes/$CLASS_ID/students/import" -F "file=@import_valid.xlsx" | show

say "4. 班级学生列表（核对入库人数与账号绑定）"
TC "$BASE/api/classes/$CLASS_ID/students?page=1&size=20" | show

say "5. 导入学生登录：$STU_NO / $STU_INIT_PWD"
SC -X POST "$BASE/api/identity/login" -H "$CT" \
   -d "{\"username\":\"$STU_NO\",\"password\":\"$STU_INIT_PWD\"}" | show

say "6. 会话恢复 /api/identity/me"
SC "$BASE/api/identity/me" | show

say "7. 未改密前访问业务接口（预期 1004 强制改密）"
SC "$BASE/api/my/scores" | show
SC "$BASE/api/my/notices" | show

say "8. 修改初始口令：$STU_INIT_PWD -> $STU_NEW_PWD"
SC -X POST "$BASE/api/identity/password" -H "$CT" \
   -d "{\"oldPassword\":\"$STU_INIT_PWD\",\"newPassword\":\"$STU_NEW_PWD\"}" | show

say "9. 用新口令重新登录（不重启会话，直接验证新口令生效）"
rm -f "$SJ"
SC -X POST "$BASE/api/identity/login" -H "$CT" \
   -d "{\"username\":\"$STU_NO\",\"password\":\"$STU_NEW_PWD\"}" | show

say "10. 改密后再次访问业务接口（预期 code=0，不再 1004）"
SC "$BASE/api/my/scores" | show

say "11. 导入 import_mixed.xlsx（2 行合法 + 5 行非法，验证逐行容错）"
TC -X POST "$BASE/api/classes/$CLASS_ID/students/import" -F "file=@import_mixed.xlsx" | show

say "12. 用非 .xlsx 文件验证格式拦截（预期 2006）"
cp import_valid.xlsx ./not_excel.txt
TC -X POST "$BASE/api/classes/$CLASS_ID/students/import" -F "file=@not_excel.txt" | show
rm -f ./not_excel.txt

say "完成"
echo "  教师端：$BASE（$TEACHER_USER / $TEACHER_PWD）"
echo "  学生端：$STU_NO / $STU_NEW_PWD"
echo "  复位：bash cleanup_imported.sh"
