# Code Defenders - Trigger Pipeline when Draft Prefix is removed from a Merge Request

This branch/commit contains a simple .gitlab-ci.yml file to trigger a pipeline on a merge request when the state of the
merge request changes from 'Draft:' to non 'Draft:'.

This is a workaround until GitLab provides this functionality out of the box.
The related issue is: https://gitlab.com/gitlab-org/gitlab/-/issues/25426

This workaround is build after https://www.youtube.com/watch?v=1u5c2PzQ9fs

There are some further settings necessary at the project level:
- A pipeline trigger configured under 'Project > Settings > CI/CD > Pipeline triggers'
- A Webhook configured under 'Project > Settings > Webhooks
- A Project Access Token configured under 'Project > Settings > Access Tokens'
