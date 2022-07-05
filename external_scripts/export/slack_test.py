from modules.Common import Slack

Slack.post_slack_msg("Test message 1", status=True)
Slack.post_slack_msg("Test message 1", status=False)