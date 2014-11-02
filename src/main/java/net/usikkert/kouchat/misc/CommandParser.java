
/***************************************************************************
 *   Copyright 2006-2014 by Christian Ihle                                 *
 *   contact@kouchat.net                                                   *
 *                                                                         *
 *   This file is part of KouChat.                                         *
 *                                                                         *
 *   KouChat is free software; you can redistribute it and/or modify       *
 *   it under the terms of the GNU Lesser General Public License as        *
 *   published by the Free Software Foundation, either version 3 of        *
 *   the License, or (at your option) any later version.                   *
 *                                                                         *
 *   KouChat is distributed in the hope that it will be useful,            *
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of        *
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU      *
 *   Lesser General Public License for more details.                       *
 *                                                                         *
 *   You should have received a copy of the GNU Lesser General Public      *
 *   License along with KouChat.                                           *
 *   If not, see <http://www.gnu.org/licenses/>.                           *
 ***************************************************************************/

package net.usikkert.kouchat.misc;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

import net.usikkert.kouchat.Constants;
import net.usikkert.kouchat.message.CoreMessages;
import net.usikkert.kouchat.net.FileReceiver;
import net.usikkert.kouchat.net.FileSender;
import net.usikkert.kouchat.net.FileTransfer;
import net.usikkert.kouchat.net.TransferList;
import net.usikkert.kouchat.settings.Settings;
import net.usikkert.kouchat.ui.UserInterface;
import net.usikkert.kouchat.util.DateTools;
import net.usikkert.kouchat.util.Tools;
import net.usikkert.kouchat.util.Validate;

import org.jetbrains.annotations.NonNls;

/**
 * Parses and executes commands. A command starts with a slash, and can
 * have arguments.
 *
 * @author Christian Ihle
 */
public class CommandParser {

    private final DateTools dateTools = new DateTools();

    private final Controller controller;
    private final UserInterface ui;
    private final MessageController msgController;
    private final User me;
    private final TransferList tList;
    private final Settings settings;
    private final CoreMessages coreMessages;

    /**
     * Constructor.
     *
     * @param controller The controller.
     * @param ui The user interface.
     * @param settings The settings to use.
     * @param coreMessages The messages to use.
     */
    public CommandParser(final Controller controller, final UserInterface ui, final Settings settings,
                         final CoreMessages coreMessages) {
        Validate.notNull(controller, "Controller can not be null");
        Validate.notNull(ui, "UserInterface can not be null");
        Validate.notNull(settings, "Settings can not be null");
        Validate.notNull(coreMessages, "Core messages can not be null");

        this.controller = controller;
        this.ui = ui;
        this.settings = settings;
        this.coreMessages = coreMessages;

        msgController = ui.getMessageController();
        me = settings.getMe();
        tList = controller.getTransferList();
    }

    /**
     * Command: <em>/topic &lt;optional new topic&gt;</em>.
     *
     * <p>Prints the current topic if no arguments are supplied,
     * or changes the topic. To remove the topic, use a space as the argument.</p>
     *
     * @param args Nothing, or the new topic.
     */
    private void cmdTopic(final String args) {
        if (args.length() == 0) {
            final Topic topic = controller.getTopic();

            if (topic.getTopic().equals("")) {
                msgController.showSystemMessage(coreMessages.getMessage("core.command.topic.systemMessage.noTopic"));
            }

            else {
                final String date = dateTools.dateToString(new Date(topic.getTime()),
                                                           coreMessages.getMessage("core.dateFormat.topic"));
                msgController.showSystemMessage(coreMessages.getMessage("core.command.topic.systemMessage.topicIs",
                                                                        topic.getTopic(), topic.getNick(), date));
            }
        }

        else {
            try {
                fixTopic(args);
            }

            catch (final CommandException e) {
                msgController.showSystemMessage(e.getMessage());
            }
        }
    }

    /**
     * Command: <em>/away &lt;away message&gt;</em>.
     *
     * <p>Set status to away.</p>
     *
     * @param args The away message.
     */
    private void cmdAway(final String args) {
        if (me.isAway()) {
            msgController.showSystemMessage(coreMessages.getMessage("core.command.away.systemMessage.alreadyAway",
                                                                    me.getAwayMsg()));
        }

        else {
            if (args.trim().length() == 0) {
                msgController.showSystemMessage(coreMessages.getMessage("core.command.away.systemMessage.missingArgument"));
            }

            else {
                try {
                    controller.goAway(args.trim());
                }

                catch (final CommandException e) {
                    msgController.showSystemMessage(e.getMessage());
                }
            }
        }
    }

    /**
     * Command: <em>/back</em>.
     *
     * <p>Set status to not away.</p>
     */
    private void cmdBack() {
        if (me.isAway()) {
            try {
                controller.comeBack();
            }

            catch (final CommandException e) {
                msgController.showSystemMessage(e.getMessage());
            }
        }

        else {
            msgController.showSystemMessage(coreMessages.getMessage("core.command.back.systemMessage.notAway"));
        }
    }

    /**
     * Command: <em>/clear</em>.
     *
     * <p>Clear all the text from the chat.</p>
     */
    private void cmdClear() {
        ui.clearChat();
    }

    /**
     * Command: <em>/about</em>.
     *
     * <p>Show information about the application.</p>
     */
    private void cmdAbout() {
        msgController.showSystemMessage(coreMessages.getMessage("core.command.about.systemMessage.about",
                                                                Constants.APP_NAME, Constants.APP_VERSION,
                                                                Constants.AUTHOR_NAME , Constants.AUTHOR_MAIL,
                                                                Constants.APP_WEB));
    }

    /**
     * Command: <em>/help</em>.
     *
     * <p>Shows a list of commands.</p>
     */
    private void cmdHelp() {
        showCommands();
    }

    /**
     * Command: <em>/whois &lt;nick&gt;</em>.
     *
     * <p>Show information about a user.</p>
     *
     * @param args The user to show information about.
     */
    private void cmdWhois(final String args) {
        if (args.trim().length() == 0) {
            msgController.showSystemMessage("/whois - missing argument <nick>");
        }

        else {
            final String[] argsArray = args.split("\\s");
            final String nick = argsArray[1].trim();

            final User user = controller.getUser(nick);

            if (user == null) {
                msgController.showSystemMessage("/whois - no such user '" + nick + "'");
            }

            else {
                String info = "/whois - " + user.getNick();

                if (user.isAway()) {
                    info += " (Away)";
                }

                info += ":\nIP address: " + user.getIpAddress();

                if (user.getHostName() != null) {
                    info += "\nHost name: " + user.getHostName();
                }

                info += "\nClient: " + user.getClient() +
                        "\nOperating System: " + user.getOperatingSystem() +
                        "\nOnline: " + dateTools.howLongFromNow(user.getLogonTime());

                if (user.isAway()) {
                    info += "\nAway message: " + user.getAwayMsg();
                }

                msgController.showSystemMessage(info);
            }
        }
    }

    /**
     * Command: <em>/send &lt;nick&gt; &lt;file&gt;</em>.
     *
     * <p>Send a file to a user.</p>
     *
     * @param args First argument is the user to send to, and the second is the file to send to the user.
     */
    private void cmdSend(final String args) {
        final String[] argsArray = args.split("\\s");

        if (argsArray.length <= 2) {
            msgController.showSystemMessage("/send - missing arguments <nick> <file>");
        }

        else {
            final String nick = argsArray[1];
            final User user = controller.getUser(nick);

            if (user != me) {
                if (user == null) {
                    msgController.showSystemMessage("/send - no such user '" + nick + "'");
                }

                else {
                    String file = "";

                    for (int i = 2; i < argsArray.length; i++) {
                        file += argsArray[i] + " ";
                    }

                    file = file.trim();
                    final File sendFile = new File(file);

                    if (sendFile.exists() && sendFile.isFile()) {
                        try {
                            sendFile(user, sendFile);
                        }

                        catch (final CommandException e) {
                            msgController.showSystemMessage(e.getMessage());
                        }
                    }

                    else {
                        msgController.showSystemMessage("/send - no such file '" + file + "'");
                    }
                }
            }

            else {
                msgController.showSystemMessage("/send - no point in doing that!");
            }
        }
    }

    /**
     * Command: <em>/receive &lt;nick&gt; &lt;id&gt;</em>.
     *
     * <p>Accept a file transfer request from a user and start the transfer.</p>
     *
     * @param args First argument is the other user in the file transfer, and the second is the id of the file transfer.
     */
    private void cmdReceive(final String args) {
        final String[] argsArray = args.split("\\s");

        if (argsArray.length != 3) {
            msgController.showSystemMessage("/receive - wrong number of arguments: <nick> <id>");
            return;
        }

        final String nick = argsArray[1];
        final User user = controller.getUser(nick);

        if (user == null) {
            msgController.showSystemMessage("/receive - no such user '" + nick + "'");
            return;
        }

        if (user == me) {
            msgController.showSystemMessage("/receive - no point in doing that!");
            return;
        }

        final Integer id = parseFileTransferId(argsArray[2]);

        if (id == null) {
            msgController.showSystemMessage("/receive - invalid file id argument: '" + argsArray[2] + "'");
            return;
        }

        final FileReceiver fileReceiver = tList.getFileReceiver(user, id);

        if (fileReceiver == null) {
            msgController.showSystemMessage("/receive - no file with id " + id + " offered by " + nick);
            return;
        }

        if (fileReceiver.isAccepted()) {
            msgController.showSystemMessage("/receive - already receiving '" + fileReceiver.getFileName() + "' from " + nick);
            return;
        }

        final File file = fileReceiver.getFile();

        if (file.exists()) {
            final File newFile = Tools.getFileWithIncrementedName(file);
            msgController.showSystemMessage("/receive - file '" + file.getName() + "' already exists - renaming to '" + newFile.getName() + "'");
            fileReceiver.setFile(newFile);
        }

        fileReceiver.accept();
    }

    /**
     * Command: <em>/reject &lt;nick&gt; &lt;id&gt;</em>.
     *
     * <p>Reject a file transfer request from a user and abort the transfer.</p>
     *
     * @param args First argument is the other user in the file transfer, and the second is the id of the file transfer.
     */
    private void cmdReject(final String args) {
        final String[] argsArray = args.split("\\s");

        if (argsArray.length != 3) {
            msgController.showSystemMessage("/reject - wrong number of arguments: <nick> <id>");
            return;
        }

        final String nick = argsArray[1];
        final User user = controller.getUser(nick);

        if (user == null) {
            msgController.showSystemMessage("/reject - no such user '" + nick + "'");
            return;
        }

        if (user == me) {
            msgController.showSystemMessage("/reject - no point in doing that!");
            return;
        }

        final Integer id = parseFileTransferId(argsArray[2]);

        if (id == null) {
            msgController.showSystemMessage("/reject - invalid file id argument: '" + argsArray[2] + "'");
            return;
        }

        final FileReceiver fileReceiver = tList.getFileReceiver(user, id);

        if (fileReceiver == null) {
            msgController.showSystemMessage("/reject - no file with id " + id + " offered by " + nick);
            return;
        }

        if (fileReceiver.isAccepted()) {
            msgController.showSystemMessage("/reject - already receiving '" + fileReceiver.getFileName() + "' from " + nick);
            return;
        }

        fileReceiver.reject();
    }

    /**
     * Command: <em>/cancel &lt;nick&gt; &lt;id&gt;</em>.
     *
     * <p>Cancel an ongoing file transfer with a user.</p>
     *
     * @param args First argument is the other user in the file transfer, and the second is the id of the file transfer.
     */
    private void cmdCancel(final String args) {
        final String[] argsArray = args.split("\\s");

        if (argsArray.length != 3) {
            msgController.showSystemMessage("/cancel - wrong number of arguments: <nick> <id>");
            return;
        }

        final String nick = argsArray[1];
        final User user = controller.getUser(nick);

        if (user == null) {
            msgController.showSystemMessage("/cancel - no such user '" + nick + "'");
            return;
        }

        if (user == me) {
            msgController.showSystemMessage("/cancel - no point in doing that!");
            return;
        }

        final Integer id = parseFileTransferId(argsArray[2]);

        if (id == null) {
            msgController.showSystemMessage("/cancel - invalid file id argument: '" + argsArray[2] + "'");
            return;
        }

        final FileTransfer fileTransfer = tList.getFileTransfer(user, id);

        if (fileTransfer == null) {
            msgController.showSystemMessage("/cancel - no file transfer with id " + id + " going on with " + nick);
            return;
        }

        if (fileTransfer instanceof FileReceiver) {
            final FileReceiver fileReceiver = (FileReceiver) fileTransfer;

            if (!fileReceiver.isAccepted()) {
                msgController.showSystemMessage("/cancel - transfer of '" + fileReceiver.getFileName() + "' from " + nick + " has not started yet");
                return;
            }
        }

        cancelFileTransfer(fileTransfer);
    }

    private Integer parseFileTransferId(final String argument) {
        try {
            return Integer.parseInt(argument);
        } catch (final NumberFormatException e) {
            return null;
        }
    }

    /**
     * Command: <em>/msg &lt;nick&gt; &lt;msg&gt;</em>.
     *
     * <p>Send a private message to a user.</p>
     *
     * @param args The first argument is the user to send to, and the second is the private message to the user.
     */
    private void cmdMsg(final String args) {
        final String[] argsArray = args.split("\\s");

        if (argsArray.length <= 2) {
            msgController.showSystemMessage("/msg - missing arguments <nick> <msg>");
        }

        else {
            final String nick = argsArray[1];
            final User user = controller.getUser(nick);

            if (user == null) {
                msgController.showSystemMessage("/msg - no such user '" + nick + "'");
            }

            else if (user == me) {
                msgController.showSystemMessage("/msg - no point in doing that!");
            }

            else if (settings.isNoPrivateChat()) {
                msgController.showSystemMessage("/msg - can't send private chat message when private chat is disabled");
            }

            else if (user.getPrivateChatPort() == 0) {
                msgController.showSystemMessage("/msg - " + user.getNick() + " can't receive private chat messages");
            }

            else {
                String privmsg = "";

                for (int i = 2; i < argsArray.length; i++) {
                    privmsg += argsArray[i] + " ";
                }

                privmsg = privmsg.trim();

                try {
                    controller.sendPrivateMessage(privmsg, user);
                    msgController.showPrivateOwnMessage(user, privmsg);
                }

                catch (final CommandException e) {
                    msgController.showSystemMessage(e.getMessage());
                }
            }
        }
    }

    /**
     * Command: <em>/nick &lt;new nick&gt;</em>.
     *
     * <p>Changes your nick name.</p>
     *
     * @param args The nick to change to.
     */
    private void cmdNick(final String args) {
        if (args.trim().length() == 0) {
            msgController.showSystemMessage("/nick - missing argument <nick>");
        }

        else {
            final String[] argsArray = args.split("\\s");
            final String nick = argsArray[1].trim();

            if (!nick.equals(me.getNick())) {
                if (controller.isNickInUse(nick)) {
                    msgController.showSystemMessage("/nick - '" + nick + "' is in use by someone else");
                }

                else if (!Tools.isValidNick(nick)) {
                    msgController.showSystemMessage("/nick - '" + nick + "' is not a valid nick name. (1-10 letters)");
                }

                else {
                    try {
                        controller.changeMyNick(nick);
                        msgController.showSystemMessage("You changed nick to " + me.getNick());
                        ui.showTopic();
                    }

                    catch (final CommandException e) {
                        msgController.showSystemMessage(e.getMessage());
                    }
                }
            }

            else {
                msgController.showSystemMessage("/nick - you are already called '" + nick + "'");
            }
        }
    }

    /**
     * Command: <em>/users</em>.
     *
     * <p>Shows a list of connected users.</p>
     */
    private void cmdUsers() {
        final UserList list = controller.getUserList();
        String userList = "";

        for (int i = 0; i < list.size(); i++) {
            final User user = list.get(i);
            userList += user.getNick();

            if (i < list.size() - 1) {
                userList += ", ";
            }
        }

        msgController.showSystemMessage("Users: " + userList);
    }

    /**
     * Command: <em>/transfers</em>.
     *
     * <p>Shows a list of all transfers and their status.</p>
     */
    private void cmdTransfers() {
        final List<FileSender> fsList = tList.getFileSenders();
        final List<FileReceiver> frList = tList.getFileReceivers();

        final StringBuilder transferInfo = new StringBuilder();

        if (fsList.size() > 0) {
            transferInfo.append("\n- Sending:");

            for (final FileSender fs : fsList) {
                appendTransferInfo(fs, transferInfo, "to");
            }
        }

        if (frList.size() > 0) {
            transferInfo.append("\n- Receiving:");

            for (final FileReceiver fr : frList) {
                appendTransferInfo(fr, transferInfo, "from");
            }
        }

        if (transferInfo.length() == 0) {
            transferInfo.append(" no active file transfers");
        }

        msgController.showSystemMessage("File transfers:" + transferInfo.toString());
    }

    /**
     * Command: <em>/quit</em>.
     *
     * <p>Quits the application.</p>
     */
    private void cmdQuit() {
        ui.quit();
    }

    /**
     * Adds a new line with information about the file transfer.
     *
     * @param fileTransfer The file transfer to add info about.
     * @param transferInfo The string builder to add the info to.
     * @param direction To or from.
     */
    private void appendTransferInfo(final FileTransfer fileTransfer, final StringBuilder transferInfo, final String direction) {
        transferInfo.append("\n  ");
        transferInfo.append("#" + fileTransfer.getId() + " ");
        transferInfo.append(fileTransfer.getFile().getName());
        transferInfo.append(" [" + Tools.byteToString(fileTransfer.getFileSize()) + "]");
        transferInfo.append(" (" + fileTransfer.getPercent() + "%, ");
        transferInfo.append(Tools.byteToString(fileTransfer.getSpeed()) + "/s)");
        transferInfo.append(" " + direction + " ");
        transferInfo.append(fileTransfer.getUser().getNick());
    }

    /**
     * Command: <em>//&lt;text&gt;</em>.
     *
     * <p>Sends the text as a message, instead of parsing it as a command.</p>
     *
     * @param line The text starting with a slash.
     */
    private void cmdSlash(final String line) {
        final String message = line.replaceFirst("/", "");

        try {
            controller.sendChatMessage(message);
            msgController.showOwnMessage(message);
        }

        catch (final CommandException e) {
            msgController.showSystemMessage(e.getMessage());
        }
    }

    /**
     * Command: <em>/'anything'</em>.
     *
     * <p>The command was not recognized by the parser.</p>
     *
     * @param command The unknown command.
     */
    private void cmdUnknown(final String command) {
        msgController.showSystemMessage("Unknown command '" + command + "'. Type /help for a list of commands");
    }

    /**
     * Updates the topic. If the new topic is empty, the topic will be removed.
     *
     * @param newTopic The new topic to use.
     * @throws CommandException If there was a problem changing the topic.
     */
    public void fixTopic(final String newTopic) throws CommandException {
        final Topic topic = controller.getTopic();
        final String trimTopic = newTopic.trim();

        if (!trimTopic.equals(topic.getTopic().trim())) {
            controller.changeTopic(trimTopic);

            if (trimTopic.length() > 0) {
                msgController.showSystemMessage(coreMessages.getMessage("core.command.topic.systemMessage.topicChanged",
                                                                        trimTopic));
            } else {
                msgController.showSystemMessage(coreMessages.getMessage("core.command.topic.systemMessage.topicRemoved"));
            }

            ui.showTopic();
        }
    }

    /**
     * Sends a file to a user.
     *
     * @param user The user to send to.
     * @param file The file to send to the user.
     * @throws CommandException If there was a problem sending the file.
     */
    public void sendFile(final User user, final File file) throws CommandException {
        controller.sendFile(user, file);
        final FileSender fileSend = tList.addFileSender(user, file);
        ui.showTransfer(fileSend);

        final String size = Tools.byteToString(file.length());
        msgController.showSystemMessage("Trying to send the file " +
                file.getName() + " (#" + fileSend.getId() + ") [" + size + "] to " + user.getNick());
    }

    /**
     * Cancels a file transfer, even if the file transfer has not been
     * answered by the other user yet.
     *
     * @param fileTransfer The file transfer to cancel.
     */
    public void cancelFileTransfer(final FileTransfer fileTransfer) {
        fileTransfer.cancel();

        if (fileTransfer instanceof FileSender) {
            final FileSender fs = (FileSender) fileTransfer;

            // This means that the other user has not answered yet
            if (fs.isWaiting()) {
                final File file = fs.getFile();
                final User user = fs.getUser();

                msgController.showSystemMessage("You cancelled sending of " +
                        file.getName() + " to " + user.getNick());
                tList.removeFileSender(fs);
                controller.sendFileAbort(user, file.hashCode(), file.getName());
            }
        }
    }

    /**
     * Shows a list of all the supported commands, with a short description.
     */
    public void showCommands() {
        msgController.showSystemMessage(
                coreMessages.getMessage("core.command.help.commands.systemMessage", Constants.APP_NAME) + "\n" +
                        coreMessages.getMessage("core.command.about.systemMessage.help", Constants.APP_NAME) + "\n" +
                        coreMessages.getMessage("core.command.away.systemMessage.help") + "\n" +
                        coreMessages.getMessage("core.command.back.systemMessage.help") + "\n" +
                        coreMessages.getMessage("core.command.cancel.systemMessage.help") + "\n" +
                        coreMessages.getMessage("core.command.clear.help.systemMessage") + "\n" +
                        coreMessages.getMessage("core.command.help.help.systemMessage") + "\n" +
                        coreMessages.getMessage("core.command.msg.help.systemMessage") + "\n" +
                        coreMessages.getMessage("core.command.nick.help.systemMessage") + "\n" +
                        coreMessages.getMessage("core.command.quit.help.systemMessage") + "\n" +
                        coreMessages.getMessage("core.command.receive.help.systemMessage") + "\n" +
                        coreMessages.getMessage("core.command.reject.help.systemMessage") + "\n" +
                        coreMessages.getMessage("core.command.send.help.systemMessage") + "\n" +
                        coreMessages.getMessage("core.command.topic.systemMessage.help") + "\n" +
                        coreMessages.getMessage("core.command.transfers.help.systemMessage") + "\n" +
                        coreMessages.getMessage("core.command.users.help.systemMessage") + "\n" +
                        coreMessages.getMessage("core.command.whois.help.systemMessage") + "\n" +
                        coreMessages.getMessage("core.command.slash.help.systemMessage"));
    }

    /**
     * Parses the line to split the command from the arguments.
     *
     * <p>The command is then checked against valid options and redirected to the appropriate method.</p>
     *
     * @param line The command in its raw form.
     */
    public void parse(final String line) {
        @NonNls final String command;

        if (line.contains(" ")) {
            command = line.substring(1, line.indexOf(' '));
        } else {
            command = line.substring(1, line.length());
        }

        if (command.length() > 0) {
            final String args = line.replaceFirst("/" + Pattern.quote(command), "");

            if (command.equals("topic")) {
                cmdTopic(args);
            } else if (command.equals("away")) {
                cmdAway(args);
            } else if (command.equals("back")) {
                cmdBack();
            } else if (command.equals("clear")) {
                cmdClear();
            } else if (command.equals("about")) {
                cmdAbout();
            } else if (command.equals("help")) {
                cmdHelp();
            } else if (command.equals("whois")) {
                cmdWhois(args);
            } else if (command.equals("send")) {
                cmdSend(args);
            } else if (command.equals("receive")) {
                cmdReceive(args);
            } else if (command.equals("reject")) {
                cmdReject(args);
            } else if (command.equals("cancel")) {
                cmdCancel(args);
            } else if (command.equals("msg")) {
                cmdMsg(args);
            } else if (command.equals("nick")) {
                cmdNick(args);
            } else if (command.equals("users")) {
                cmdUsers();
            } else if (command.equals("transfers")) {
                cmdTransfers();
            } else if (command.equals("quit")) {
                cmdQuit();
            } else if (command.startsWith("/")) {
                cmdSlash(line);
            } else {
                cmdUnknown(command);
            }
        }

        else {
            cmdUnknown(command);
        }
    }
}
