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
package io.github.reckart.inception.humanprotocol.model;

import static java.util.Arrays.asList;

import java.util.List;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * <pre>
 * <code>
 *     {
 *       "wallet": <string>,
 *       "tasks": ['<string:task_id>']
 *     }
 * </code>
 * </pre>
 */
public class PayoutItem
{
    private String wallet;
    private List<String> taskIds;
    
    public PayoutItem()
    {
        // Nothing to do
    }

    public PayoutItem(String aWallet, List<String> aTaskIds)
    {
        wallet = aWallet;
        taskIds = aTaskIds;
    }

    public PayoutItem(String aWallet, String... aTaskIds)
    {
        wallet = aWallet;
        if (aTaskIds != null) {
            taskIds = asList(aTaskIds);
        }
    }

    public String getWallet()
    {
        return wallet;
    }

    public void setWallet(String aWallet)
    {
        wallet = aWallet;
    }

    public List<String> getTaskIds()
    {
        return taskIds;
    }

    public void setTaskIds(List<String> aTaskIds)
    {
        taskIds = aTaskIds;
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this, ToStringStyle.NO_CLASS_NAME_STYLE)
                .appendSuper(super.toString()).append("wallet", wallet).append("taskIds", taskIds)
                .toString();
    }
}
