/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.fineract.mopane.ssb.data;

import java.util.Date;

public class SsbExportRowData {

    private Long loanId;
    private String accountNo;
    private Long clientId;
    private String idNumber;
    private String ecNumber;
    private String surname;
    private String firstName;
    private String type;
    private Date startDate;
    private Date endDate;
    private Long amountCents;
    private Integer loanStatusId;
    private String skipReason;
    private boolean skipped;

    public Long getLoanId() {
        return loanId;
    }

    public void setLoanId(final Long loanId) {
        this.loanId = loanId;
    }

    public String getAccountNo() {
        return accountNo;
    }

    public void setAccountNo(final String accountNo) {
        this.accountNo = accountNo;
    }

    public Long getClientId() {
        return clientId;
    }

    public void setClientId(final Long clientId) {
        this.clientId = clientId;
    }

    public String getIdNumber() {
        return idNumber;
    }

    public void setIdNumber(final String idNumber) {
        this.idNumber = idNumber;
    }

    public String getEcNumber() {
        return ecNumber;
    }

    public void setEcNumber(final String ecNumber) {
        this.ecNumber = ecNumber;
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(final String surname) {
        this.surname = surname;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(final String firstName) {
        this.firstName = firstName;
    }

    public String getType() {
        return type;
    }

    public void setType(final String type) {
        this.type = type;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(final Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(final Date endDate) {
        this.endDate = endDate;
    }

    public Long getAmountCents() {
        return amountCents;
    }

    public void setAmountCents(final Long amountCents) {
        this.amountCents = amountCents;
    }

    public Integer getLoanStatusId() {
        return loanStatusId;
    }

    public void setLoanStatusId(final Integer loanStatusId) {
        this.loanStatusId = loanStatusId;
    }

    public String getSkipReason() {
        return skipReason;
    }

    public void setSkipReason(final String skipReason) {
        this.skipReason = skipReason;
    }

    public boolean isSkipped() {
        return skipped;
    }

    public void setSkipped(final boolean skipped) {
        this.skipped = skipped;
    }
}
