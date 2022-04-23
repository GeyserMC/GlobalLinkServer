/*
 * Copyright (c) 2021-2021 GeyserMC. http://geysermc.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 * @author GeyserMC
 * @link https://github.com/GeyserMC/GlobalLinkServer
 */

package org.geysermc.globallinkserver.bedrock;

import com.google.gson.JsonObject;
import com.nukkitx.network.util.DisconnectReason;
import com.nukkitx.protocol.bedrock.BedrockPacketCodec;
import com.nukkitx.protocol.bedrock.BedrockServerSession;
import com.nukkitx.protocol.bedrock.handler.BedrockPacketHandler;
import com.nukkitx.protocol.bedrock.packet.*;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.geysermc.cumulus.CustomForm;
import org.geysermc.cumulus.Form;
import org.geysermc.cumulus.SimpleForm;
import org.geysermc.cumulus.response.CustomFormResponse;
import org.geysermc.cumulus.response.SimpleFormResponse;
import org.geysermc.globallinkserver.bedrock.util.BedrockVersionUtils;
import org.geysermc.globallinkserver.link.LinkManager;
import org.geysermc.globallinkserver.player.PlayerManager;
import org.geysermc.globallinkserver.util.CommandUtils;
import org.geysermc.globallinkserver.util.Utils;

public class PacketHandler implements BedrockPacketHandler {
    private static final int LINK_ACCOUNT_FORM_ID = 1;
    private static final int UNLINK_ACCOUNT_FORM_ID = 2;
    private static final int LINK_ACCOUNT_SHOW_CODE = 3;
    private static final int LINK_ACCOUNT_ENTER_CODE = 4;

    private static final SimpleForm LINK_ACCOUNT_FORM = SimpleForm.builder()
            .title("Global Link Server")
            .content("Welcome to the Global Link Server.")
            .button("Start link process")
            .button("Continue link process from Java Edition")
            .button("Disconnect")
            .build();
    private static final CustomForm LINK_ACCOUNT_ENTER_CODE_FORM = CustomForm.builder()
            .title("Global Link Server")
            .input("Enter the code you received from Java Edition:", "0000")
            .build();

    private final BedrockServerSession session;
    private final PlayerManager playerManager;
    private final LinkManager linkManager;

    private BedrockPlayer player;
    private boolean loggedIn = false;

    private final Int2ObjectMap<Form> forms = new Int2ObjectOpenHashMap<>(2);

    public PacketHandler(
            BedrockServerSession session,
            PlayerManager playerManager,
            LinkManager linkManager) {
        this.session = session;
        this.playerManager = playerManager;
        this.linkManager = linkManager;

        session.addDisconnectHandler(this::disconnect);
    }

    public void disconnect(DisconnectReason reason) {
        if (player != null) {
            playerManager.removeBedrockPlayer(player);
        }
    }

    private void sendForm(int formId, Form form) {
        ModalFormRequestPacket packet = new ModalFormRequestPacket();
        packet.setFormId(formId);
        packet.setFormData(form.getJsonData());
        session.sendPacket(packet);
        forms.put(formId, form);
    }

    @Override
    public boolean handle(LoginPacket packet) {
        BedrockPacketCodec packetCodec =
                BedrockVersionUtils.getBedrockCodec(packet.getProtocolVersion());

        if (packetCodec == null) {
            PlayStatusPacket status = new PlayStatusPacket();
            if (packet.getProtocolVersion() > BedrockVersionUtils.getLatestProtocolVersion()) {
                status.setStatus(PlayStatusPacket.Status.LOGIN_FAILED_SERVER_OLD);
            } else {
                status.setStatus(PlayStatusPacket.Status.LOGIN_FAILED_CLIENT_OLD);
            }
            session.sendPacket(status);
            session.disconnect();
            return true;
        }

        session.setPacketCodec(packetCodec);

        try {
            JsonObject extraData = Utils.validateData(
                    packet.getChainData().toString(),
                    packet.getSkinData().toString()
            );

            player = playerManager.addBedrockPlayer(session, extraData);

            PlayStatusPacket status = new PlayStatusPacket();
            status.setStatus(PlayStatusPacket.Status.LOGIN_SUCCESS);
            session.sendPacket(status);

            ResourcePacksInfoPacket info = new ResourcePacksInfoPacket();
            session.sendPacket(info);
        } catch (AssertionError | Exception error) {
            session.disconnect("disconnect.loginFailed");
        }
        return true;
    }

    @Override
    public boolean handle(ResourcePackClientResponsePacket packet) {
        switch (packet.getStatus()) {
            case COMPLETED:
                player.sendStartGame();
                break;
            case HAVE_ALL_PACKS:
                ResourcePackStackPacket stack = new ResourcePackStackPacket();
                stack.setExperimentsPreviouslyToggled(false);
                stack.setForcedToAccept(false);
                stack.setGameVersion("*");
                session.sendPacket(stack);
                break;
            default:
                session.disconnect("disconnectionScreen.resourcePack");
                break;
        }
        return true;
    }

    @Override
    public boolean handle(SetLocalPlayerAsInitializedPacket packet) {
        if (loggedIn) {
            return true;
        }
        loggedIn = true;
        linkManager.findLinkForBedrock(player.getUniqueId()).whenComplete((result, error) -> {
            if (result == null) {
                sendForm(LINK_ACCOUNT_FORM_ID, LINK_ACCOUNT_FORM);
                return;
            }

            sendForm(UNLINK_ACCOUNT_FORM_ID, SimpleForm.builder().title("Global Link Server")
                    .content("You are already linked to the Java player " + result + ".")
                    .button("Unlink Account")
                    .button("Disconnect")
                    .build());
        });
        return true;
    }

    @Override
    public boolean handle(ModalFormResponsePacket packet) {
        int formId = packet.getFormId();
        Form form = forms.remove(formId);
        if (form == null) {
            return true;
        }

        switch (formId) {
            case LINK_ACCOUNT_FORM_ID: {
                SimpleFormResponse response = LINK_ACCOUNT_FORM.parseResponse(packet.getFormData());
                if (!response.isCorrect()) {
                    player.disconnect("Closed"); //todo
                    break;
                }

                int buttonId = response.getClickedButtonId();
                if (buttonId == 0) {
                    String code = CommandUtils.startAccountLink(player, linkManager);
                    sendForm(LINK_ACCOUNT_SHOW_CODE, SimpleForm.builder().title("Global Link Server")
                            .content(player.formatMessage("&aPlease join on Java and run `&9/linkaccount &3" + code + "&a`"))
                            .build());
                } else if (buttonId == 1) {
                    sendForm(LINK_ACCOUNT_ENTER_CODE, LINK_ACCOUNT_ENTER_CODE_FORM);
                } else {
                    player.disconnect("Closed");
                }
                break;
            }
            case LINK_ACCOUNT_ENTER_CODE: {
                CustomFormResponse response = LINK_ACCOUNT_ENTER_CODE_FORM.parseResponse(packet.getFormData());
                if (!response.isCorrect()) {
                    // Send them back
                    sendForm(LINK_ACCOUNT_FORM_ID, LINK_ACCOUNT_FORM);
                    break;
                }
                int code = Utils.parseInt(response.getInput(0));
                if (code >= 0) {
                    String message = CommandUtils.linkAccount(player, linkManager, playerManager, code);
                    if (message != null) {
                        // TODO send form
                    }
                }
            }
            case UNLINK_ACCOUNT_FORM_ID: {
                SimpleFormResponse response = (SimpleFormResponse) form.parseResponse(packet.getFormData());
                if (!response.isCorrect()) {
                    player.disconnect("Closed"); //todo
                    break;
                }

                if (response.getClickedButtonId() == 0) {
                    CommandUtils.unlinkAccount(player, linkManager);
                }
                player.disconnect("Closed");
                break;
            }
        }
        return true;
    }
}
