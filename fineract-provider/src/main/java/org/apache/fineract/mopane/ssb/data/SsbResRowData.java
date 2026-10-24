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

public class SsbResRowData {

    private Long id;
    private Long batchId;
    private int rowNumber;
    private String recId;
    private String deductionCode;
    private String reference;
    private String idNumber;
    private String ecNumber;
    private String type;
    private String bureauStatus;
    private Date startDate;
    private Date endDate;
    private BigDecimal amount;
    private String name;
    private String message;
    private String status;
    private String reason;
    private Long suggestedLoanId;
    private String suggestedAccountNo;
    private Long loanId;
    private Long disbursementTransactionId;
    private Long noteId;
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

    public String getType() {
        return type;
    }

    public void setType(final String type) {
        this.type = type;
    }

    public String getBureauStatus() {
        return bureauStatus;
    }

    public void setBureauStatus(final String bureauStatus) {
        this.bureauStatus = bureauStatus;
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

    public String getMessage() {
        return message;
    }

    public void setMessage(final String message) {
        this.message = message;
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

    public Long getLoanId() {
        return loanId;
    }

    public void setLoanId(final Long loanId) {
        this.loanId = loanId;
    }

    public Long getDisbursementTransactionId() {
        return disbursementTransactionId;
    }

    public void setDisbursementTransactionId(final Long disbursementTransactionId) {
        this.disbursementTransactionId = disbursementTransactionId;
    }

    public Long getNoteId() {
        return noteId;
    }

    public void setNoteId(final Long noteId) {
        this.noteId = noteId;
    }

    public String getNote() {
        return note;
    }

    public void setNote(final String note) {
        this.note = note;
    }
}
