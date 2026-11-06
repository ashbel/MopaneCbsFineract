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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class SsbResBatchData {

    private Long id;
    private String bureau;
    private String filename;
    private Long uploadedBy;
    private Date uploadedOn;
    private boolean dryRun;
    private boolean autoDisburse;
    private Long paymentTypeId;
    private int disbursedCount;
    private int authorisedCount;
    private int notedCount;
    private int failedCount;
    private int needsReviewCount;
    private int totalCount;
    private List<SsbResRowData> rows = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(final Long id) {
        this.id = id;
    }

    public String getBureau() {
        return bureau;
    }

    public void setBureau(final String bureau) {
        this.bureau = bureau;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(final String filename) {
        this.filename = filename;
    }

    public Long getUploadedBy() {
        return uploadedBy;
    }

    public void setUploadedBy(final Long uploadedBy) {
        this.uploadedBy = uploadedBy;
    }

    public Date getUploadedOn() {
        return uploadedOn;
    }

    public void setUploadedOn(final Date uploadedOn) {
        this.uploadedOn = uploadedOn;
    }

    public boolean isDryRun() {
        return dryRun;
    }

    public void setDryRun(final boolean dryRun) {
        this.dryRun = dryRun;
    }

    public boolean isAutoDisburse() {
        return autoDisburse;
    }

    public void setAutoDisburse(final boolean autoDisburse) {
        this.autoDisburse = autoDisburse;
    }

    public Long getPaymentTypeId() {
        return paymentTypeId;
    }

    public void setPaymentTypeId(final Long paymentTypeId) {
        this.paymentTypeId = paymentTypeId;
    }

    public int getDisbursedCount() {
        return disbursedCount;
    }

    public void setDisbursedCount(final int disbursedCount) {
        this.disbursedCount = disbursedCount;
    }

    public int getAuthorisedCount() {
        return authorisedCount;
    }

    public void setAuthorisedCount(final int authorisedCount) {
        this.authorisedCount = authorisedCount;
    }

    public int getNotedCount() {
        return notedCount;
    }

    public void setNotedCount(final int notedCount) {
        this.notedCount = notedCount;
    }

    public int getFailedCount() {
        return failedCount;
    }

    public void setFailedCount(final int failedCount) {
        this.failedCount = failedCount;
    }

    public int getNeedsReviewCount() {
        return needsReviewCount;
    }

    public void setNeedsReviewCount(final int needsReviewCount) {
        this.needsReviewCount = needsReviewCount;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(final int totalCount) {
        this.totalCount = totalCount;
    }

    public List<SsbResRowData> getRows() {
        return rows;
    }

    public void setRows(final List<SsbResRowData> rows) {
        this.rows = rows;
    }
}
