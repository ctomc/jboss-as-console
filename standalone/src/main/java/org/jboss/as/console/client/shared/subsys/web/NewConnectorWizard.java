/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @author tags. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */

package org.jboss.as.console.client.shared.subsys.web;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.jboss.as.console.client.shared.subsys.web.model.HttpConnector;
import org.jboss.as.console.client.widgets.DialogueOptions;
import org.jboss.as.console.client.widgets.forms.CheckBoxItem;
import org.jboss.as.console.client.widgets.forms.Form;
import org.jboss.as.console.client.widgets.forms.FormValidation;
import org.jboss.as.console.client.widgets.forms.TextBoxItem;

import java.util.List;

/**
 * @author Heiko Braun
 * @date 5/12/11
 */
public class NewConnectorWizard {
    private WebPresenter presenter;
    private List<HttpConnector> connectors;

    public NewConnectorWizard(WebPresenter presenter, List<HttpConnector> connectors) {
        this.presenter = presenter;
        this.connectors = connectors;
    }

    Widget asWidget() {
        VerticalPanel layout = new VerticalPanel();
        layout.setStyleName("fill-layout-width");
        final Form<HttpConnector> form = new Form<HttpConnector>(HttpConnector.class);

        TextBoxItem name = new TextBoxItem("name", "Name");

        // todo: turn into pull down. But how is the the socket-binding group resolved?
        TextBoxItem socket = new TextBoxItem("socketBinding", "Socket Binding") {

            private String errOrig;

            @Override
            public boolean validate(String value) {


                boolean parentValid = super.validate(value);
                boolean bindingValid = true;
                if(parentValid)
                {
                    for(HttpConnector existing : connectors)
                    {
                        if(existing.getSocketBinding().equals(value))
                        {
                            errOrig = getErrMessage();
                            setErrMessage("Socket binding already in use");
                            bindingValid = false;
                        }
                    }
                }

                return parentValid && bindingValid;
            }
        };

        TextBoxItem protocol = new TextBoxItem("protocol", "Protocol");
        TextBoxItem scheme = new TextBoxItem("scheme", "Scheme");

        CheckBoxItem enabled = new CheckBoxItem("enabled", "Enabled?");

        // defaults
        protocol.setValue("http");
        scheme.setValue("http");
        enabled.setValue(Boolean.TRUE);

        form.setFields(name,enabled,socket,protocol,scheme);

        layout.add(form.asWidget());

        DialogueOptions options = new DialogueOptions(
            new ClickHandler() {

                @Override
                public void onClick(ClickEvent event) {

                    FormValidation validation = form.validate();
                    if(!validation.hasErrors())
                        presenter.onCreateConnector(form.getUpdatedEntity());
                }
            },
             new ClickHandler() {

                @Override
                public void onClick(ClickEvent event) {
                   presenter.closeDialogue();
                }
            }
        );

        layout.add(options);

        return layout;
    }
}
