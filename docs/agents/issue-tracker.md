# Issue tracker: GitHub

本仓库的任务和规格存放在 GitHub Issues，通过 gh CLI 操作。
在仓库目录内运行命令，由 git remote 确定目标仓库。

## 常用操作

- 创建：gh issue create --title "..." --body-file <path>
- 读取：gh issue view <number> --comments
- 列表：gh issue list --state open --json number,title,body,labels
- 评论：gh issue comment <number> --body-file <path>
- 添加标签：gh issue edit <number> --add-label "..."
- 移除标签：gh issue edit <number> --remove-label "..."
- 关闭：gh issue close <number>

多行正文写入临时文件，通过 --body-file 传入。
按需使用 --label 和 --state 筛选任务。

当技能要求“publish to the issue tracker”时，创建 GitHub issue。
当技能要求“fetch the relevant ticket”时，读取 issue 及其评论。

## Pull requests as a triage surface

**PRs as a request surface: no.**

## Wayfinding

地图是带有 wayfinder:map 标签的 issue，正文保存 Notes、
Decisions-so-far 和 Fog。子任务使用 wayfinder:research、
wayfinder:prototype、wayfinder:grilling 或 wayfinder:task 标签。

优先使用 GitHub sub-issues 关联地图与子任务；不可用时，在地图正文
维护任务列表，并在子任务正文开头写入 Part of #<map>。

优先使用 GitHub 原生 issue dependencies 表示阻塞关系；
不可用时，在任务正文开头记录 Blocked by: #<n>, #<n>。
全部阻塞任务关闭后，该任务才可开始。

从地图中按顺序选择未关闭、无未关闭阻塞项且无人负责的子任务。
领取任务时使用 gh issue edit <number> --add-assignee @me。
完成时发布结果评论、关闭任务，并将结论摘要与链接写入地图的
Decisions-so-far。
