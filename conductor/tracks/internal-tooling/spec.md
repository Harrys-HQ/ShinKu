# Track Specification: Internal Tooling

## Objective
Provide automated scripts to maintain the ShinKu project, focusing on synchronization and surgical integration of upstream fixes.

## Requirements
1.  Automate the fetching of all remotes (`sy`, `mihon`).
2.  Provide a clear comparison of the local state vs. upstreams.
3.  Simplify the process of applying single commits from Mihon.
4.  Remind developers of branding-critical files to avoid regressions.

## Scope
- PowerShell scripts for Windows environments.
- Git workflow automation.
