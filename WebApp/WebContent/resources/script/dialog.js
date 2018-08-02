/**
 * Functionality for dialogs
 */

/**
 * Display the dialog, with optional content
 * @param html
 * @returns
 */
function displayDialog(html) {
  if (html) {
    $("#messageText").html(html);
  }
  PF('msgDialog').show();
}

/**
 * Generate HTML from a JSON array with messages. The JSON structure looks like
 * this:
 * [
 *   {
 *     "message":"Some message title",
 *     "details":"details of the message"
 *   },
 *   {
 *     "message":"Another message title",
 *     "details":"details of the message"
 *   }
 * ]
 *
 * @param messages
 * @returns
 */
function getMessagesHTML(messages) {
  var html = $('<ul/>').addClass('messages');
  for (var i = 0; i < messages.length; i++) {
    var row = $('<li/>');
    row.addClass("message");
    var message = $('<span/>');
    message = $('<h3/>');
    message.text(messages[i].message);
    var details = $('<div/>');
    details.html(messages[i].details.replace(/(?:\r\n|\r|\n)/g, '<br>'))
    row.append(message)
    row.append(details)
    html.append(row);
  }
  return html;
}
