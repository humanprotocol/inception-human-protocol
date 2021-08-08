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
 */
"use strict";

const Web3Modal = window.Web3Modal.default;
const WalletConnectProvider = window.WalletConnectProvider.default;
const evmChains = window.evmChains;

/**
 * Web3modal instance
 */
let web3Modal

/**
 * Chosen wallet provider given by the dialog window
 */ 
let provider;

/** 
 * Address of the selected account.
 * 
 * @type {string}
 */ 
let selectedAccount;

function init() {
  document.querySelector("#field-signature").value = "";
  document.querySelector(".btn-login").disabled = true;
  document.querySelector("#waiting-for-signature-msg").textContent = 
    "Please choose an account to proceed.";

  const providerOptions = {
    walletconnect: {
      package: WalletConnectProvider,
      options: {
        infuraId: "DUMMY-ID", // FIXME: Allow injecting ID from backend
      }
    }
  };
  web3Modal = new Web3Modal({ 
    cacheProvider: false, // optional
    providerOptions, // required
   });
}

async function fetchAccountData() {
  const web3 = new Web3(provider);

  // Get first account
  const accounts = await web3.eth.getAccounts();
  selectedAccount = accounts[0];

  document.querySelector(".username").value = selectedAccount;
  document.querySelector("#field-signature").value = "";
  document.querySelector(".btn-login").disabled = true;
  document.querySelector("#waiting-for-signature-msg").textContent = 
    "The join button will activate once you have proven your identity by providing the requested signature."
  web3.eth.personal.sign(document.querySelector("#field-signature-request").value, selectedAccount)
    .then(onSigned, onSignatureAborted)
}

async function onSigned(signedMessage) {
  document.querySelector("#field-signature").value = signedMessage;
  document.querySelector(".btn-login").disabled = false;
  document.querySelector("#waiting-for-signature-msg").textContent = 
    "Thank you! You can now join the project.";
}

async function onSignatureAborted(signedMessage) {
  document.querySelector("#field-signature").value = "";
  document.querySelector(".btn-login").disabled = true;
  document.querySelector("#waiting-for-signature-msg").textContent = 
    "You cannot join the project unless you prove your identity. Please use the switch account link above to get back to choosing an account.";
}


/**
 * Connect wallet button pressed.
 */
async function onConnect() {
  if (provider) {
    if (provider.close) {
      await provider.close();

      // Required to make WalletConnect ask for the QR code agai.
      await web3Modal.clearCachedProvider();
      provider = null;
    }
  }

  console.log("Opening a dialog", web3Modal);
  try {
    provider = await web3Modal.connect();
  } catch(e) {
    console.log("Could not get a wallet connection", e);
    return;
  }

  // Subscribe to accounts change
  provider.on("accountsChanged", (accounts) => {
    fetchAccountData();
  });

  await fetchAccountData(provider);
}

/**
 * Main entry point.
 */
window.addEventListener('load', async () => {
  init();
  onConnect();
  document.querySelector("#btn-switch-account").addEventListener("click", onConnect);
});
