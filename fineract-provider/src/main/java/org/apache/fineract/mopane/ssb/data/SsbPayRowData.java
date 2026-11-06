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

import java.math.BigDecimal;
import java.util.Date;

public class SsbPayRowData {

    private Long id;
    private Long batchId;
    private int rowNumber;
    private String recId;
    private String deductionCode;
    private String reference;
    private String idNumber;
    private String ecNumber;
    private Date transDate;
    private BigDecimal amount;
    private String name;
    private String status;
    private String reason;
    private Long suggestedLoanId;
    private String suggestedAccountNo;
    private Long postedLoanId;
    private Long postedTransactionId;
    private String note;

    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public Long getBatchId() {
        return batchId;
    }

    public void setBatchId(final Long batchId) {
        this.batchId = batchId;
    }

    public int getRowNumber() {
        return rowNumber;
    }

    public void setRowNumber(final int rowNumber) {
        this.rowNumber = rowNumber;
    }

    public String getRecId() {
        return recId;
    }

    public void setRecId(final String recId) {
        this.recId = recId;
    }

    public String getDeductionCode() {
        return deductionCode;
    }

    public void setDeductionCode(final String deductionCode) {
        this.deductionCode = deductionCode;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(final String reference) {
        this.reference = reference;
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

    public Date getTransDate() {
        return transDate;
    }

    public void setTransDate(final Date transDate) {
        this.transDate = transDate;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(final BigDecimal amount) {
        this.amount = amount;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(final String status) {
        this.status = status;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(final String reason) {
        this.reason = reason;
    }

    public Long getSuggestedLoanId() {
        return suggestedLoanId;
    }

    public void setSuggestedLoanId(final Long suggestedLoanId) {
        this.suggestedLoanId = suggestedLoanId;
    }

    public String getSuggestedAccountNo() {
        return suggestedAccountNo;
    }

    public void setSuggestedAccountNo(final String suggestedAccountNo) {
        this.suggestedAccountNo = suggestedAccountNo;
    }

    public Long getPostedLoanId() {
        return postedLoanId;
    }

    public void setPostedLoanId(final Long postedLoanId) {
        this.postedLoanId = postedLoanId;
    }

    public Long getPostedTransactionId() {
        return postedTransactionId;
    }

    public void setPostedTransactionId(final Long postedTransactionId) {
        this.postedTransactionId = postedTransactionId;
    }

    public String getNote() {
        return note;
    }

    public void setNote(final String note) {
        this.note = note;
    }
}
