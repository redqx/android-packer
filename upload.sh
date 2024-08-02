#!/bin/bash

# 获取输入参数
commit_msg="$1"

# 如果输入参数为空,则使用默认提交信息
if [ -z "$commit_msg" ]; then
    commit_msg="default upload"
fi

# 执行Git命令
git add .
git commit -m "$commit_msg"
git push -u origin master