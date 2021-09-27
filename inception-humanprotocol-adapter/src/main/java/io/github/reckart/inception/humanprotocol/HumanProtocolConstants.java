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
package io.github.reckart.inception.humanprotocol;

public interface HumanProtocolConstants
{
   String HEADER_X_HUMAN_SIGNATURE = "X-human-signature";
   
   String HEADER_X_EXCHANGE_SIGNATURE= "X-exchange-signature";
   
   String INVITE_LINK_ENDPOINT = "/exchange/job/invite-link";
   
   String JOB_RESULTS_ENDPOINT = "/exchange/job/results";
   
   String UUID_PATTERN = "^[0-9A-Fa-f]{8}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{12}$";
   String INFURA_PATTERN = "^[0-9A-Fa-f]+$";
   
   String TASK_TYPE_SPAN_SELECT = "span_select";
   String TASK_TYPE_DOCUMENT_TAGGING = "document_tagging";
   
   String REQUEST_CONFIG_KEY_PROJECT_TITLE = "projectTitle";
   String REQUEST_CONFIG_KEY_CROSS_SENENCE = "crossSenence";
   String REQUEST_CONFIG_KEY_OVERLAP = "overlap";
   String REQUEST_CONFIG_KEY_ANCHORING = "anchoring";
   String REQUEST_CONFIG_KEY_VERSION = "version";
   String REQUEST_CONFIG_DATA_FORMAT = "dataFormat";
   
   String OVERLAP_NONE = "none";
   String OVERLAP_ANY = "any";

   String ANCHORING_TOKENS = "tokens";
   String ANCHORING_SENTENCES = "sentences";
   String ANCHORING_DOCUMENTS = "documents";
}
