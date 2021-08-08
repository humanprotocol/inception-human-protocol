/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *         http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. 
 * 
 * ------------------------------------------------------------------------
 * 
 * This file has been adapted from the original AcceptInvitePage class as
 * provided by INCEpTION 20.1.
 */
package io.github.reckart.inception.humanprotocol.page;

import static de.tudarmstadt.ukp.clarin.webanno.model.PermissionLevel.ANNOTATOR;
import static de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaBehavior.visibleWhen;
import static de.tudarmstadt.ukp.inception.sharing.model.Mandatoriness.MANDATORY;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.wicket.markup.head.JavaScriptHeaderItem.forReference;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.IFeedback;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.EmailTextField;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.HiddenField;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.session.SessionRegistry;
import org.web3j.crypto.ECDSASignature;
import org.web3j.crypto.Hash;
import org.web3j.crypto.Keys;
import org.web3j.crypto.Sign;
import org.web3j.crypto.Sign.SignatureData;
import org.web3j.utils.Numeric;

import com.github.rjeschke.txtmark.Processor;

import de.tudarmstadt.ukp.clarin.webanno.api.ProjectService;
import de.tudarmstadt.ukp.clarin.webanno.model.ProjectPermission;
import de.tudarmstadt.ukp.clarin.webanno.security.UserDao;
import de.tudarmstadt.ukp.clarin.webanno.security.model.User;
import de.tudarmstadt.ukp.clarin.webanno.support.lambda.LambdaAjaxButton;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.ApplicationSession;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.login.LoginProperties;
import de.tudarmstadt.ukp.clarin.webanno.ui.core.page.ProjectPageBase;
import de.tudarmstadt.ukp.inception.sharing.InviteService;
import de.tudarmstadt.ukp.inception.sharing.config.InviteServiceProperties;
import de.tudarmstadt.ukp.inception.sharing.model.ProjectInvite;
import de.tudarmstadt.ukp.inception.ui.core.dashboard.project.ProjectDashboardPage;

public class AcceptInvitePage
    extends ProjectPageBase
{
    private static final long serialVersionUID = 5160703195387357692L;

    public static final String PAGE_PARAM_INVITE_ID = "i";

    private @SpringBean InviteService inviteService;
    private @SpringBean ProjectService projectService;
    private @SpringBean UserDao userRepository;
    private @SpringBean LoginProperties loginProperties;
    private @SpringBean SessionRegistry sessionRegistry;
    private @SpringBean InviteServiceProperties inviteServiceProperties;

    private final IModel<FormData> formModel;
    private final IModel<ProjectInvite> invite;
    private final IModel<Boolean> invitationIsValid;
    private final WebMarkupContainer tooManyUsersNotice;
    
    private final String signatureRequest;

    public AcceptInvitePage(final PageParameters aPageParameters)
    {
        super(aPageParameters);

        invitationIsValid = LoadableDetachableModel.of(this::checkInvitation);
        invite = LoadableDetachableModel.of(() -> inviteService.readProjectInvite(getProject()));

        User user = userRepository.getCurrentUser();

        // If the current user has already accepted the invitation, directly forward to the project
        if (user != null && invitationIsValid.getObject()) {
            if (projectService.existsProjectPermissionLevel(user, getProject(), ANNOTATOR)) {
                backToProjectPage();
            }
        }

        add(new WebMarkupContainer("expirationNotice")
                .add(visibleWhen(() -> !invitationIsValid.orElse(false).getObject())));

        tooManyUsersNotice = new WebMarkupContainer("tooManyUsersNotice");
        tooManyUsersNotice.add(visibleWhen(this::isTooManyUsers));
        add(tooManyUsersNotice);

        signatureRequest = "Please sign this random message to prove your identity.\n"
                + UUID.randomUUID();
        formModel = new CompoundPropertyModel<>(new FormData());
        formModel.getObject().signatureRequest = signatureRequest;

        Form<FormData> form = new Form<>("acceptInvitationForm", formModel);
        form.add(new RequiredTextField<String>("username") //
                .add(AttributeModifier.replace("readonly", "readonly"))
                .add(AttributeModifier.replace("placeholder",
                        LoadableDetachableModel.of(this::getUserIdPlaceholder)))
                .add(visibleWhen(() -> user == null)));
        form.add(new HiddenField<>("signatureRequest", String.class));
        form.add(new HiddenField<>("signature", String.class));
        form.add(new EmailTextField("eMail") //
                .setRequired(invite.getObject().getAskForEMail() == MANDATORY)
                .add(visibleWhen(() -> user == null && formModel.getObject().askForEMail)));
        form.add(new LambdaAjaxButton<>("join", this::actionJoinProject));
        form.add(new Label("invitationText", LoadableDetachableModel.of(this::getInvitationText))
                .setEscapeModelStrings(false));

        form.add(visibleWhen(
                () -> invitationIsValid.orElse(false).getObject() && !isTooManyUsers()));
        add(form);
    }

    @Override
    public void renderHead(IHeaderResponse aResponse)
    {
        super.renderHead(aResponse);

        aResponse.render(forReference(Web3ResourceReference.get()));
        aResponse.render(forReference(Web3ModalResourceReference.get()));
        aResponse.render(forReference(EvmChainsResourceReference.get()));
        aResponse.render(forReference(Web3ProviderResourceReference.get()));
        aResponse.render(forReference(FortmaticResourceReference.get()));
        aResponse.render(forReference(AcceptInvitePageJavascriptResourceReference.get()));
    }

    private String getUserIdPlaceholder()
    {
        return invite.getObject().getUserIdPlaceholder();
    }

    private String getInvitationText()
    {
        if (invite.getObject() == null) {
            return "Invitation does not exist.";
        }

        String invitationText;
        if (isBlank(invite.getObject().getInvitationText())) {
            invitationText = String.join("\n", //
                    "## Welcome!", //
                    "", //
                    "You have been invited to join the project", //
                    "**" + getProject().getName() + "**", //
                    "as an annotator.", //
                    "", //
                    "Would you like to join?");
        }
        else {
            invitationText = invite.getObject().getInvitationText();
        }

        return Processor.process(invitationText, true);
    }

    private String getInviteId()
    {
        return getPageParameters().get(PAGE_PARAM_INVITE_ID).toOptionalString();
    }

    private boolean checkInvitation()
    {
        return getProject() != null && inviteService.isValidInviteLink(getProject(), getInviteId());
    }

    private void actionJoinProject(AjaxRequestTarget aTarget, Form<FormData> aForm)
    {
        if (!checkInvitation()) {
            error("Invitation has expired.");
            aTarget.addChildren(getPage(), IFeedback.class);
            return;
        }

        FormData formData = aForm.getModelObject();
        if (!isSignatureValid(formData.username, formData.signature, signatureRequest)) {
            error("Invalid signature");
            aTarget.addChildren(getPage(), IFeedback.class);
            return;
        }

        User user = signInAsProjectUser(aForm.getModelObject());

        if (user == null) {
            error("Login failed");
            aTarget.addChildren(getPage(), IFeedback.class);
            return;
        }

        setResponsePage(ProjectDashboardPage.class, new PageParameters()
                .set(ProjectDashboardPage.PAGE_PARAM_PROJECT, getProject().getId()));
    }

    private Authentication asAuthentication(User aUser)
    {
        Set<GrantedAuthority> authorities = userRepository.listAuthorities(aUser).stream()
                .map(_role -> new SimpleGrantedAuthority(_role.getAuthority()))
                .collect(Collectors.toSet());
        return new UsernamePasswordAuthenticationToken(aUser.getUsername(), null, authorities);

    }

    private User signInAsProjectUser(FormData aFormData)
    {
        Optional<User> existingUser = inviteService.getProjectUser(getProject(),
                aFormData.username);
        String storedEMail = existingUser.map(User::getEmail).orElse(null);
        if (storedEMail != null && !storedEMail.equals(aFormData.eMail)) {
            error("Provided eMail address does not match stored eMail address");
            return null;
        }

        User user = inviteService.getOrCreateProjectUser(getProject(), aFormData.username);
        if (aFormData.eMail != null && user.getEmail() == null) {
            user.setEmail(aFormData.eMail);
            userRepository.update(user);
        }

        // Want to make sure we clear any session-bound state
        ApplicationSession.get().signIn(asAuthentication(user));
        createProjectPermissionsIfNecessary(user);

        return user;
    }

    private void createProjectPermissionsIfNecessary(User aUser)
    {
        if (!projectService.existsProjectPermissionLevel(aUser, getProject(), ANNOTATOR)) {
            projectService.createProjectPermission(
                    new ProjectPermission(getProject(), aUser.getUsername(), ANNOTATOR));
            getSession().success("You have successfully joined the project.");
        }
        else {
            getSession().info("You were already an annotator on this project.");
        }
    }

    /**
     * Check if settings property is set and there will be more users logged in (with current one)
     * than max users allowed.
     */
    private boolean isTooManyUsers()
    {
        long maxUsers = loginProperties.getMaxConcurrentSessions();
        return maxUsers > 0 && sessionRegistry.getAllPrincipals().size() >= maxUsers;
    }

    private boolean isSignatureValid(String address, String signature, String message)
    {
        String prefix = "\u0019Ethereum Signed Message:\n" + message.length();
        byte[] msgHash = Hash.sha3((prefix + message).getBytes());

        byte[] signatureBytes = Numeric.hexStringToByteArray(signature);
        byte v = signatureBytes[64];
        if (v < 27) {
            v += 27;
        }

        SignatureData sd = new SignatureData( //
                v,  //
                (byte[]) Arrays.copyOfRange(signatureBytes, 0, 32), //
                (byte[]) Arrays.copyOfRange(signatureBytes, 32, 64));

        // Iterate for each possible key to recover
        for (int i = 0; i < 4; i++) {
            BigInteger publicKey = Sign.recoverFromSignature((byte) i,
                    new ECDSASignature(new BigInteger(1, sd.getR()), new BigInteger(1, sd.getS())),
                    msgHash);

            if (publicKey != null) {
                String addressRecovered = "0x" + Keys.getAddress(publicKey);

                if (addressRecovered.equals(address.toLowerCase(Locale.ROOT))) {
                    return true;
                }
            }
        }

        return false;
    }

    private static class FormData
        implements Serializable
    {
        private static final long serialVersionUID = -2338711546557816393L;

        String username;
        String signatureRequest;
        String signature;
        String eMail;
        boolean askForEMail;
    }
}
