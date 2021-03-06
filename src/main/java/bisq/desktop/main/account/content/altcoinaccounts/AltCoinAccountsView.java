/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.desktop.main.account.content.altcoinaccounts;

import bisq.desktop.common.view.ActivatableViewAndModel;
import bisq.desktop.common.view.FxmlView;
import bisq.desktop.components.AutoTooltipButton;
import bisq.desktop.components.AutoTooltipLabel;
import bisq.desktop.components.TitledGroupBg;
import bisq.desktop.components.paymentmethods.CryptoCurrencyForm;
import bisq.desktop.components.paymentmethods.PaymentMethodForm;
import bisq.desktop.main.overlays.popups.Popup;
import bisq.desktop.util.FormBuilder;
import bisq.desktop.util.ImageUtil;
import bisq.desktop.util.Layout;

import bisq.core.locale.CryptoCurrency;
import bisq.core.locale.Res;
import bisq.core.locale.TradeCurrency;
import bisq.core.payment.AccountAgeWitnessService;
import bisq.core.payment.PaymentAccount;
import bisq.core.payment.PaymentAccountFactory;
import bisq.core.payment.payload.PaymentMethod;
import bisq.core.payment.validation.AltCoinAddressValidator;
import bisq.core.util.BSFormatter;
import bisq.core.util.validation.InputValidator;

import bisq.common.UserThread;
import bisq.common.util.Tuple2;
import bisq.common.util.Tuple3;

import javax.inject.Inject;

import javafx.stage.Stage;

import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;

import javafx.geometry.VPos;

import javafx.beans.value.ChangeListener;

import javafx.util.Callback;

import java.util.concurrent.TimeUnit;

import static bisq.desktop.util.FormBuilder.add2ButtonsAfterGroup;
import static bisq.desktop.util.FormBuilder.add3ButtonsAfterGroup;
import static bisq.desktop.util.FormBuilder.addTitledGroupBg;

@FxmlView
public class AltCoinAccountsView extends ActivatableViewAndModel<GridPane, AltCoinAccountsViewModel> {

    private ListView<PaymentAccount> paymentAccountsListView;

    private final InputValidator inputValidator;
    private final AltCoinAddressValidator altCoinAddressValidator;
    private final AccountAgeWitnessService accountAgeWitnessService;
    private final BSFormatter formatter;

    private PaymentMethodForm paymentMethodForm;
    private TitledGroupBg accountTitledGroupBg;
    private Button addAccountButton, saveNewAccountButton, exportButton, importButton;
    private int gridRow = 0;
    private ChangeListener<PaymentAccount> paymentAccountChangeListener;

    @Inject
    public AltCoinAccountsView(AltCoinAccountsViewModel model,
                               InputValidator inputValidator,
                               AltCoinAddressValidator altCoinAddressValidator,
                               AccountAgeWitnessService accountAgeWitnessService,
                               BSFormatter formatter) {
        super(model);

        this.inputValidator = inputValidator;
        this.altCoinAddressValidator = altCoinAddressValidator;
        this.accountAgeWitnessService = accountAgeWitnessService;
        this.formatter = formatter;
    }

    @Override
    public void initialize() {
        buildForm();
        paymentAccountChangeListener = (observable, oldValue, newValue) -> {
            if (newValue != null)
                onSelectAccount(newValue);
        };
        Label placeholder = new AutoTooltipLabel(Res.get("shared.noAccountsSetupYet"));
        placeholder.setWrapText(true);
        paymentAccountsListView.setPlaceholder(placeholder);
    }

    @Override
    protected void activate() {
        paymentAccountsListView.setItems(model.getPaymentAccounts());
        paymentAccountsListView.getSelectionModel().selectedItemProperty().addListener(paymentAccountChangeListener);
        addAccountButton.setOnAction(event -> addNewAccount());
        exportButton.setOnAction(event -> model.dataModel.exportAccounts((Stage) root.getScene().getWindow()));
        importButton.setOnAction(event -> model.dataModel.importAccounts((Stage) root.getScene().getWindow()));
    }

    @Override
    protected void deactivate() {
        paymentAccountsListView.getSelectionModel().selectedItemProperty().removeListener(paymentAccountChangeListener);
        addAccountButton.setOnAction(null);
        exportButton.setOnAction(null);
        importButton.setOnAction(null);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////
    // UI actions
    ///////////////////////////////////////////////////////////////////////////////////////////

    private void onSaveNewAccount(PaymentAccount paymentAccount) {
        TradeCurrency selectedTradeCurrency = paymentAccount.getSelectedTradeCurrency();
        if (selectedTradeCurrency != null) {
            String code = selectedTradeCurrency.getCode();
            if (selectedTradeCurrency instanceof CryptoCurrency && ((CryptoCurrency) selectedTradeCurrency).isAsset()) {
                String name = selectedTradeCurrency.getName();
                new Popup<>().information(Res.get("account.altcoin.popup.wallet.msg",
                        selectedTradeCurrency.getCodeAndName(),
                        name,
                        name))
                        .closeButtonText(Res.get("account.altcoin.popup.wallet.confirm"))
                        .show();
            }

            switch (code) {
                case "XMR":
                    new Popup<>().information(Res.get("account.altcoin.popup.xmr.msg"))
                            .useIUnderstandButton()
                            .show();
                    break;
                case "CCX":
                    new Popup<>().information(Res.get("account.altcoin.popup.ccx.msg"))
                            .useIUnderstandButton()
                            .show();
                    break;
                case "ZEC":
                    new Popup<>().information(Res.get("account.altcoin.popup.ZEC.msg", "ZEC"))
                            .useIUnderstandButton()
                            .show();
                    break;
                case "XZC":
                    new Popup<>().information(Res.get("account.altcoin.popup.XZC.msg", "XZC"))
                            .useIUnderstandButton()
                            .show();
                    break;
                case "BCH":
                case "BCHC":
                    new Popup<>().information(Res.get("account.altcoin.popup.bch"))
                            .useIUnderstandButton()
                            .show();
                    break;
                case "BTG":
                    new Popup<>().information(Res.get("account.altcoin.popup.btg"))
                            .useIUnderstandButton()
                            .show();
                    break;
            }

            if (!model.getPaymentAccounts().stream().filter(e -> e.getAccountName() != null &&
                    e.getAccountName().equals(paymentAccount.getAccountName())).findAny().isPresent()) {
                model.onSaveNewAccount(paymentAccount);
                removeNewAccountForm();
            } else {
                new Popup<>().warning(Res.get("shared.accountNameAlreadyUsed")).show();
            }
        }
    }

    private void onCancelNewAccount() {
        removeNewAccountForm();
    }

    private void onDeleteAccount(PaymentAccount paymentAccount) {
        new Popup<>().warning(Res.get("shared.askConfirmDeleteAccount"))
                .actionButtonText(Res.get("shared.yes"))
                .onAction(() -> {
                    boolean isPaymentAccountUsed = model.onDeleteAccount(paymentAccount);
                    if (!isPaymentAccountUsed)
                        removeSelectAccountForm();
                    else
                        UserThread.runAfter(() -> new Popup<>().warning(
                                Res.get("shared.cannotDeleteAccount"))
                                .show(), 100, TimeUnit.MILLISECONDS);
                })
                .closeButtonText(Res.get("shared.cancel"))
                .show();
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    // Base form
    ///////////////////////////////////////////////////////////////////////////////////////////

    private void buildForm() {
        addTitledGroupBg(root, gridRow, 1, Res.get("shared.manageAccounts"));

        Tuple2<Label, ListView<PaymentAccount>> tuple = FormBuilder.addLabelListView(root, gridRow, Res.get("account.altcoin.yourAltcoinAccounts"), Layout.FIRST_ROW_DISTANCE);
        GridPane.setValignment(tuple.first, VPos.TOP);
        paymentAccountsListView = tuple.second;
        paymentAccountsListView.setPrefHeight(2 * Layout.LIST_ROW_HEIGHT + 14);
        paymentAccountsListView.setCellFactory(new Callback<ListView<PaymentAccount>, ListCell<PaymentAccount>>() {
            @Override
            public ListCell<PaymentAccount> call(ListView<PaymentAccount> list) {
                return new ListCell<PaymentAccount>() {
                    final Label label = new AutoTooltipLabel();
                    final ImageView icon = ImageUtil.getImageViewById(ImageUtil.REMOVE_ICON);
                    final Button removeButton = new AutoTooltipButton("", icon);
                    final AnchorPane pane = new AnchorPane(label, removeButton);

                    {
                        label.setLayoutY(5);
                        removeButton.setId("icon-button");
                        AnchorPane.setRightAnchor(removeButton, 0d);
                    }

                    @Override
                    public void updateItem(final PaymentAccount item, boolean empty) {
                        super.updateItem(item, empty);
                        if (item != null && !empty) {
                            label.setText(item.getAccountName());
                            removeButton.setOnAction(e -> onDeleteAccount(item));
                            setGraphic(pane);
                        } else {
                            setGraphic(null);
                        }
                    }
                };
            }
        });

        Tuple3<Button, Button, Button> tuple3 = add3ButtonsAfterGroup(root, ++gridRow, Res.get("shared.addNewAccount"),
                Res.get("shared.ExportAccounts"), Res.get("shared.importAccounts"));
        addAccountButton = tuple3.first;
        exportButton = tuple3.second;
        importButton = tuple3.third;
    }

    // Add new account form
    private void addNewAccount() {
        paymentAccountsListView.getSelectionModel().clearSelection();
        removeAccountRows();
        addAccountButton.setDisable(true);
        accountTitledGroupBg = addTitledGroupBg(root, ++gridRow, 1, Res.get("shared.createNewAccount"), Layout.GROUP_DISTANCE);

        if (paymentMethodForm != null) {
            FormBuilder.removeRowsFromGridPane(root, 3, paymentMethodForm.getGridRow() + 1);
            GridPane.setRowSpan(accountTitledGroupBg, paymentMethodForm.getRowSpan() + 1);
        }
        gridRow = 2;
        paymentMethodForm = getPaymentMethodForm(PaymentMethod.BLOCK_CHAINS);
        paymentMethodForm.addFormForAddAccount();
        gridRow = paymentMethodForm.getGridRow();
        Tuple2<Button, Button> tuple2 = add2ButtonsAfterGroup(root, ++gridRow, Res.get("shared.saveNewAccount"), Res.get("shared.cancel"));
        saveNewAccountButton = tuple2.first;
        saveNewAccountButton.setOnAction(event -> onSaveNewAccount(paymentMethodForm.getPaymentAccount()));
        saveNewAccountButton.disableProperty().bind(paymentMethodForm.allInputsValidProperty().not());
        Button cancelButton = tuple2.second;
        cancelButton.setOnAction(event -> onCancelNewAccount());
        GridPane.setRowSpan(accountTitledGroupBg, paymentMethodForm.getRowSpan() + 1);
    }

    // Select account form
    private void onSelectAccount(PaymentAccount paymentAccount) {
        removeAccountRows();
        addAccountButton.setDisable(false);
        accountTitledGroupBg = addTitledGroupBg(root, ++gridRow, 1, Res.get("shared.selectedAccount"), Layout.GROUP_DISTANCE);
        paymentMethodForm = getPaymentMethodForm(paymentAccount);
        paymentMethodForm.addFormForDisplayAccount();
        gridRow = paymentMethodForm.getGridRow();
        Tuple2<Button, Button> tuple = add2ButtonsAfterGroup(root, ++gridRow, Res.get("shared.deleteAccount"), Res.get("shared.cancel"));
        Button deleteAccountButton = tuple.first;
        deleteAccountButton.setOnAction(event -> onDeleteAccount(paymentMethodForm.getPaymentAccount()));
        Button cancelButton = tuple.second;
        cancelButton.setOnAction(event -> removeSelectAccountForm());
        GridPane.setRowSpan(accountTitledGroupBg, paymentMethodForm.getRowSpan());
        model.onSelectAccount(paymentAccount);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////
    // Utils
    ///////////////////////////////////////////////////////////////////////////////////////////

    private PaymentMethodForm getPaymentMethodForm(PaymentMethod paymentMethod) {
        final PaymentAccount paymentAccount = PaymentAccountFactory.getPaymentAccount(paymentMethod);
        paymentAccount.init();
        return getPaymentMethodForm(paymentAccount);
    }

    private PaymentMethodForm getPaymentMethodForm(PaymentAccount paymentAccount) {
        return new CryptoCurrencyForm(paymentAccount, accountAgeWitnessService, altCoinAddressValidator, inputValidator, root, gridRow, formatter);
    }

    private void removeNewAccountForm() {
        saveNewAccountButton.disableProperty().unbind();
        removeAccountRows();
        addAccountButton.setDisable(false);
    }

    private void removeSelectAccountForm() {
        FormBuilder.removeRowsFromGridPane(root, 2, gridRow);
        gridRow = 1;
        addAccountButton.setDisable(false);
        paymentAccountsListView.getSelectionModel().clearSelection();
    }


    private void removeAccountRows() {
        FormBuilder.removeRowsFromGridPane(root, 2, gridRow);
        gridRow = 1;
    }

}

