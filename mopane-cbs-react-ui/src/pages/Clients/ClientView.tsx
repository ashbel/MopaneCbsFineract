import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Link, useParams } from 'react-router-dom';
import { fetchClient, transferClient } from '../../api/clients';
import {
  fetchClientCharges,
  fetchClientChargeTemplate,
  addClientCharge,
  waiveClientCharge,
  payClientCharge,
  type ClientChargeSummary,
} from '../../api/clientCharges';
import {
  fetchClientIdentifiers,
  fetchClientIdentifierTemplate,
  addClientIdentifier,
  updateClientIdentifier,
  deleteClientIdentifier,
  type ClientIdentifierSummary,
} from '../../api/clientIdentifiers';
import {
  fetchClientDocuments,
  uploadClientDocument,
  downloadClientDocument,
  deleteClientDocument,
  type ClientDocumentSummary,
} from '../../api/clientDocuments';
import { fetchOffices } from '../../api/offices';
import './ClientView.css';

function formatChargeDate(arr: number[] | undefined): string {
  if (!arr || arr.length < 3) return '—';
  return `${arr[0]}-${String(arr[1]).padStart(2, '0')}-${String(arr[2]).padStart(2, '0')}`;
}

export default function ClientView() {
  const { id } = useParams<{ id: string }>();
  const clientId = Number(id);
  const queryClient = useQueryClient();
  const [chargeModalOpen, setChargeModalOpen] = useState(false);
  const [chargeId, setChargeId] = useState('');
  const [chargeAmount, setChargeAmount] = useState('');
  const [chargeDueDate, setChargeDueDate] = useState(() => new Date().toISOString().slice(0, 10));
  const [payChargeId, setPayChargeId] = useState<number | null>(null);
  const [payAmount, setPayAmount] = useState('');
  const [payDate, setPayDate] = useState(() => new Date().toISOString().slice(0, 10));
  const [identifierModalOpen, setIdentifierModalOpen] = useState(false);
  const [editingIdentifierId, setEditingIdentifierId] = useState<number | null>(null);
  const [identifierTypeId, setIdentifierTypeId] = useState('');
  const [identifierKey, setIdentifierKey] = useState('');
  const [identifierDesc, setIdentifierDesc] = useState('');
  const [sectionError, setSectionError] = useState('');
  const [documentModalOpen, setDocumentModalOpen] = useState(false);
  const [documentFile, setDocumentFile] = useState<File | null>(null);
  const [documentName, setDocumentName] = useState('');
  const [documentDesc, setDocumentDesc] = useState('');
  const [downloadingId, setDownloadingId] = useState<number | null>(null);
  const [transferModalOpen, setTransferModalOpen] = useState(false);
  const [transferOfficeId, setTransferOfficeId] = useState('');

  const { data: client, isLoading, error } = useQuery({
    queryKey: ['client', clientId],
    queryFn: () => fetchClient(clientId),
    enabled: Number.isFinite(clientId),
  });

  const { data: chargesData } = useQuery({
    queryKey: ['clientCharges', clientId],
    queryFn: () => fetchClientCharges(clientId),
    enabled: Number.isFinite(clientId),
  });

  const { data: chargeTemplate } = useQuery({
    queryKey: ['clientChargeTemplate', clientId],
    queryFn: () => fetchClientChargeTemplate(clientId),
    enabled: Number.isFinite(clientId) && chargeModalOpen,
  });

  const { data: identifiers } = useQuery({
    queryKey: ['clientIdentifiers', clientId],
    queryFn: () => fetchClientIdentifiers(clientId),
    enabled: Number.isFinite(clientId),
  });

  const { data: identifierTemplate } = useQuery({
    queryKey: ['clientIdentifierTemplate', clientId],
    queryFn: () => fetchClientIdentifierTemplate(clientId),
    enabled: Number.isFinite(clientId) && identifierModalOpen,
  });

  const { data: documents } = useQuery({
    queryKey: ['clientDocuments', clientId],
    queryFn: () => fetchClientDocuments(clientId),
    enabled: Number.isFinite(clientId),
  });

  const { data: offices } = useQuery({
    queryKey: ['offices'],
    queryFn: () => fetchOffices(),
    enabled: transferModalOpen,
  });

  const addChargeMutation = useMutation({
    mutationFn: (body: { chargeId: number; amount: number; dueDate?: string }) =>
      addClientCharge(clientId, body),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['clientCharges', clientId] });
      setChargeModalOpen(false);
      setChargeId('');
      setChargeAmount('');
      setSectionError('');
    },
    onError: () => setSectionError('Failed to add charge.'),
  });

  const waiveChargeMutation = useMutation({
    mutationFn: (cId: number) => waiveClientCharge(clientId, cId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['clientCharges', clientId] });
      setSectionError('');
    },
    onError: () => setSectionError('Failed to waive charge.'),
  });

  const payChargeMutation = useMutation({
    mutationFn: ({ cId, amount, paymentDate }: { cId: number; amount: number; paymentDate: string }) =>
      payClientCharge(clientId, cId, { amount, paymentDate }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['clientCharges', clientId] });
      setPayChargeId(null);
      setPayAmount('');
      setSectionError('');
    },
    onError: () => setSectionError('Failed to pay charge.'),
  });

  const addIdentifierMutation = useMutation({
    mutationFn: (body: { documentTypeId: number; documentKey: string; description?: string }) =>
      addClientIdentifier(clientId, body),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['clientIdentifiers', clientId] });
      setIdentifierModalOpen(false);
      setEditingIdentifierId(null);
      setIdentifierTypeId('');
      setIdentifierKey('');
      setIdentifierDesc('');
      setSectionError('');
    },
    onError: () => setSectionError('Failed to add identifier.'),
  });

  const updateIdentifierMutation = useMutation({
    mutationFn: ({ identifierId, body }: { identifierId: number; body: { documentKey?: string; description?: string } }) =>
      updateClientIdentifier(clientId, identifierId, body),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['clientIdentifiers', clientId] });
      setIdentifierModalOpen(false);
      setEditingIdentifierId(null);
      setIdentifierKey('');
      setIdentifierDesc('');
      setSectionError('');
    },
    onError: () => setSectionError('Failed to update identifier.'),
  });

  const deleteIdentifierMutation = useMutation({
    mutationFn: (identifierId: number) => deleteClientIdentifier(clientId, identifierId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['clientIdentifiers', clientId] });
      setSectionError('');
    },
    onError: () => setSectionError('Failed to delete identifier.'),
  });

  const uploadDocumentMutation = useMutation({
    mutationFn: (formData: FormData) => uploadClientDocument(clientId, formData),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['clientDocuments', clientId] });
      setDocumentModalOpen(false);
      setDocumentFile(null);
      setDocumentName('');
      setDocumentDesc('');
      setSectionError('');
    },
    onError: () => setSectionError('Failed to upload document.'),
  });

  const deleteDocumentMutation = useMutation({
    mutationFn: (documentId: number) => deleteClientDocument(clientId, documentId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['clientDocuments', clientId] });
      setSectionError('');
    },
    onError: () => setSectionError('Failed to delete document.'),
  });

  const transferMutation = useMutation({
    mutationFn: (destinationOfficeId: number) =>
      transferClient(clientId, { destinationOfficeId }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['client', clientId] });
      queryClient.invalidateQueries({ queryKey: ['clients'] });
      setTransferModalOpen(false);
      setTransferOfficeId('');
      setSectionError('');
    },
    onError: () => setSectionError('Transfer failed. Please try again.'),
  });

  async function handleDownloadDocument(doc: ClientDocumentSummary) {
    setDownloadingId(doc.id);
    setSectionError('');
    try {
      const blob = await downloadClientDocument(clientId, doc.id);
      const fileName = doc.fileName ?? doc.name ?? `document-${doc.id}`;
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = fileName;
      a.click();
      URL.revokeObjectURL(url);
    } catch {
      setSectionError('Failed to download document.');
    } finally {
      setDownloadingId(null);
    }
  }

  const charges = chargesData?.pageItems ?? [];
  const chargeOptions = chargeTemplate?.chargeOptions ?? [];
  const identifierTypeOptions = identifierTemplate?.allowedDocumentTypes ?? [];

  if (!Number.isFinite(clientId)) {
    return (
      <div className="page-error">
        Invalid client ID. <Link to="/clients">Back to clients</Link>
      </div>
    );
  }

  if (isLoading) return <div className="page-loading">Loading client…</div>;
  if (error || !client) {
    return (
      <div className="page-error">
        Failed to load client. <Link to="/clients">Back to clients</Link>
      </div>
    );
  }

  return (
    <div className="client-view-page">
      <div className="page-header">
        <div>
          <Link to="/clients" className="back-link">← Clients</Link>
          <h1>{client.displayName ?? client.fullName ?? `Client ${client.id}`}</h1>
        </div>
        <div className="page-header-actions">
          <button type="button" className="btn-secondary" onClick={() => { setTransferModalOpen(true); setSectionError(''); setTransferOfficeId(''); }}>
            Transfer client
          </button>
          <Link to={`/clients/${client.id}/edit`} className="btn-primary">Edit</Link>
        </div>
      </div>
      <div className="client-detail-card">
        <h2>Details</h2>
        <dl className="detail-list">
          <div><dt>ID</dt><dd>{client.id}</dd></div>
          <div><dt>Account #</dt><dd>{client.accountNo ?? '—'}</dd></div>
          <div><dt>First name</dt><dd>{client.firstname ?? '—'}</dd></div>
          <div><dt>Last name</dt><dd>{client.lastname ?? '—'}</dd></div>
          <div><dt>Office</dt><dd>{client.officeName ?? '—'}</dd></div>
          <div><dt>Status</dt><dd>{client.status?.value ?? (client.active ? 'Active' : 'Inactive')}</dd></div>
          <div><dt>Mobile</dt><dd>{client.mobileNo ?? '—'}</dd></div>
          <div><dt>External ID</dt><dd>{client.externalId ?? '—'}</dd></div>
        </dl>
      </div>

      <div className="client-section-card">
        <h2>Charges</h2>
        {sectionError && <div className="form-error" role="alert">{sectionError}</div>}
        <div className="section-actions">
          <button type="button" className="btn-primary" onClick={() => { setChargeModalOpen(true); setSectionError(''); }}>
            Add charge
          </button>
        </div>
        <table className="client-charges-table">
          <thead>
            <tr>
              <th>Charge</th>
              <th>Amount</th>
              <th>Outstanding</th>
              <th>Due date</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {charges.length === 0 ? (
              <tr><td colSpan={5}>No charges.</td></tr>
            ) : (
              charges.map((c: ClientChargeSummary) => (
                <tr key={c.id}>
                  <td>{c.name ?? '—'}</td>
                  <td>{c.amount != null ? Number(c.amount).toLocaleString() : '—'}</td>
                  <td>{c.amountOutstanding != null ? Number(c.amountOutstanding).toLocaleString() : '—'}</td>
                  <td>{formatChargeDate(c.dueDate as number[])}</td>
                  <td>
                    {(c.amountOutstanding ?? 0) > 0 && (
                      <>
                        <button type="button" className="link-button" onClick={() => { setPayChargeId(c.id); setPayAmount(String(c.amountOutstanding ?? '')); }}>Pay</button>
                        {' '}
                        <button type="button" className="link-button" onClick={() => waiveChargeMutation.mutate(c.id)} disabled={waiveChargeMutation.isPending}>Waive</button>
                      </>
                    )}
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {chargeModalOpen && (
        <div className="modal-overlay" onClick={() => !addChargeMutation.isPending && setChargeModalOpen(false)}>
          <div className="modal-card" onClick={(e) => e.stopPropagation()}>
            <h3>Add charge</h3>
            <form onSubmit={(e) => {
              e.preventDefault();
              setSectionError('');
              const cId = Number(chargeId);
              const amount = Number(chargeAmount);
              if (!Number.isFinite(cId) || cId <= 0) { setSectionError('Select a charge.'); return; }
              if (!Number.isFinite(amount) || amount <= 0) { setSectionError('Enter a valid amount.'); return; }
              addChargeMutation.mutate({ chargeId: cId, amount, dueDate: chargeDueDate });
            }}>
              <label>
                <span>Charge</span>
                <select value={chargeId} onChange={(e) => setChargeId(e.target.value)} required disabled={addChargeMutation.isPending}>
                  <option value="">Select charge</option>
                  {chargeOptions.map((o: { id: number; name?: string }) => (
                    <option key={o.id} value={o.id}>{o.name ?? `Charge ${o.id}`}</option>
                  ))}
                </select>
              </label>
              <label>
                <span>Amount</span>
                <input type="number" step="any" min="0" value={chargeAmount} onChange={(e) => setChargeAmount(e.target.value)} required disabled={addChargeMutation.isPending} />
              </label>
              <label>
                <span>Due date</span>
                <input type="date" value={chargeDueDate} onChange={(e) => setChargeDueDate(e.target.value)} disabled={addChargeMutation.isPending} />
              </label>
              <div className="form-actions">
                <button type="button" className="btn-secondary" onClick={() => setChargeModalOpen(false)} disabled={addChargeMutation.isPending}>Cancel</button>
                <button type="submit" className="btn-primary" disabled={addChargeMutation.isPending}>Add</button>
              </div>
            </form>
          </div>
        </div>
      )}

      {payChargeId != null && (
        <div className="modal-overlay" onClick={() => !payChargeMutation.isPending && setPayChargeId(null)}>
          <div className="modal-card" onClick={(e) => e.stopPropagation()}>
            <h3>Pay charge</h3>
            <form onSubmit={(e) => {
              e.preventDefault();
              setSectionError('');
              const amount = Number(payAmount);
              if (!Number.isFinite(amount) || amount <= 0) { setSectionError('Enter a valid amount.'); return; }
              payChargeMutation.mutate({ cId: payChargeId, amount, paymentDate: payDate });
            }}>
              <label>
                <span>Amount</span>
                <input type="number" step="any" min="0" value={payAmount} onChange={(e) => setPayAmount(e.target.value)} required disabled={payChargeMutation.isPending} />
              </label>
              <label>
                <span>Payment date</span>
                <input type="date" value={payDate} onChange={(e) => setPayDate(e.target.value)} required disabled={payChargeMutation.isPending} />
              </label>
              <div className="form-actions">
                <button type="button" className="btn-secondary" onClick={() => setPayChargeId(null)} disabled={payChargeMutation.isPending}>Cancel</button>
                <button type="submit" className="btn-primary" disabled={payChargeMutation.isPending}>Pay</button>
              </div>
            </form>
          </div>
        </div>
      )}

      <div className="client-section-card">
        <h2>Identifiers</h2>
        <div className="section-actions">
          <button type="button" className="btn-primary" onClick={() => { setEditingIdentifierId(null); setIdentifierKey(''); setIdentifierDesc(''); setIdentifierTypeId(''); setIdentifierModalOpen(true); setSectionError(''); }}>
            Add identifier
          </button>
        </div>
        <table className="client-identifiers-table">
          <thead>
            <tr>
              <th>Type</th>
              <th>Key / number</th>
              <th>Description</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {(identifiers ?? []).length === 0 ? (
              <tr><td colSpan={4}>No identifiers.</td></tr>
            ) : (
              (identifiers ?? []).map((i: ClientIdentifierSummary) => (
                <tr key={i.id}>
                  <td>{i.documentType?.name ?? '—'}</td>
                  <td>{i.documentKey ?? '—'}</td>
                  <td>{i.description ?? '—'}</td>
                  <td>
                    <button type="button" className="link-button" onClick={() => { setEditingIdentifierId(i.id); setIdentifierTypeId(String(i.documentType?.id ?? '')); setIdentifierKey(i.documentKey ?? ''); setIdentifierDesc(i.description ?? ''); setIdentifierModalOpen(true); }}>Edit</button>
                    {' '}
                    <button type="button" className="link-button" onClick={() => deleteIdentifierMutation.mutate(i.id)} disabled={deleteIdentifierMutation.isPending}>Delete</button>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      <div className="client-section-card">
        <h2>Documents</h2>
        {sectionError && <div className="form-error" role="alert">{sectionError}</div>}
        <div className="section-actions">
          <button type="button" className="btn-primary" onClick={() => { setDocumentModalOpen(true); setDocumentFile(null); setDocumentName(''); setDocumentDesc(''); setSectionError(''); }}>
            Upload document
          </button>
        </div>
        <table className="client-documents-table">
          <thead>
            <tr>
              <th>Name</th>
              <th>File name</th>
              <th>Size</th>
              <th>Type</th>
              <th>Description</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {(documents ?? []).length === 0 ? (
              <tr><td colSpan={6}>No documents.</td></tr>
            ) : (
              (documents ?? []).map((d: ClientDocumentSummary) => (
                <tr key={d.id}>
                  <td>{d.name ?? '—'}</td>
                  <td>{d.fileName ?? '—'}</td>
                  <td>{d.size != null ? `${Number(d.size).toLocaleString()} B` : '—'}</td>
                  <td>{d.type ?? '—'}</td>
                  <td>{d.description ?? '—'}</td>
                  <td>
                    <button type="button" className="link-button" onClick={() => handleDownloadDocument(d)} disabled={downloadingId === d.id}>
                      {downloadingId === d.id ? 'Downloading…' : 'Download'}
                    </button>
                    {' '}
                    <button type="button" className="link-button" onClick={() => window.confirm('Delete this document?') && deleteDocumentMutation.mutate(d.id)} disabled={deleteDocumentMutation.isPending}>Delete</button>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {documentModalOpen && (
        <div className="modal-overlay" onClick={() => !uploadDocumentMutation.isPending && setDocumentModalOpen(false)}>
          <div className="modal-card" onClick={(e) => e.stopPropagation()}>
            <h3>Upload document</h3>
            <form onSubmit={(e) => {
              e.preventDefault();
              setSectionError('');
              if (!documentFile) { setSectionError('Select a file.'); return; }
              const name = documentName.trim() || documentFile.name;
              if (!name) { setSectionError('Enter a name.'); return; }
              const formData = new FormData();
              formData.append('file', documentFile);
              formData.append('name', name);
              if (documentDesc.trim()) formData.append('description', documentDesc.trim());
              uploadDocumentMutation.mutate(formData);
            }}>
              <label>
                <span>File *</span>
                <input
                  type="file"
                  onChange={(e) => {
                    const f = e.target.files?.[0];
                    setDocumentFile(f ?? null);
                    if (f && !documentName) setDocumentName(f.name);
                  }}
                  required
                  disabled={uploadDocumentMutation.isPending}
                />
              </label>
              <label>
                <span>Name *</span>
                <input value={documentName} onChange={(e) => setDocumentName(e.target.value)} placeholder="Document name" disabled={uploadDocumentMutation.isPending} />
              </label>
              <label>
                <span>Description</span>
                <input value={documentDesc} onChange={(e) => setDocumentDesc(e.target.value)} disabled={uploadDocumentMutation.isPending} />
              </label>
              <div className="form-actions">
                <button type="button" className="btn-secondary" onClick={() => setDocumentModalOpen(false)} disabled={uploadDocumentMutation.isPending}>Cancel</button>
                <button type="submit" className="btn-primary" disabled={uploadDocumentMutation.isPending || !documentFile}>Upload</button>
              </div>
            </form>
          </div>
        </div>
      )}

      {transferModalOpen && client && (
        <div className="modal-overlay" onClick={() => !transferMutation.isPending && setTransferModalOpen(false)}>
          <div className="modal-card" onClick={(e) => e.stopPropagation()}>
            <h3>Transfer client</h3>
            <form onSubmit={(e) => {
              e.preventDefault();
              setSectionError('');
              const destId = Number(transferOfficeId);
              if (!Number.isFinite(destId) || destId <= 0) { setSectionError('Select destination office.'); return; }
              if (destId === client.officeId) { setSectionError('Select a different office.'); return; }
              transferMutation.mutate(destId);
            }}>
              {sectionError && <div className="form-error" role="alert">{sectionError}</div>}
              <label>
                <span>Destination office</span>
                <select value={transferOfficeId} onChange={(e) => setTransferOfficeId(e.target.value)} required disabled={transferMutation.isPending}>
                  <option value="">Select office</option>
                  {(offices ?? []).filter((o) => o.id !== client.officeId).map((o) => (
                    <option key={o.id} value={o.id}>{o.name ?? `Office ${o.id}`}</option>
                  ))}
                </select>
              </label>
              <div className="form-actions">
                <button type="button" className="btn-secondary" onClick={() => setTransferModalOpen(false)} disabled={transferMutation.isPending}>Cancel</button>
                <button type="submit" className="btn-primary" disabled={transferMutation.isPending}>{transferMutation.isPending ? 'Transferring…' : 'Transfer'}</button>
              </div>
            </form>
          </div>
        </div>
      )}

      {identifierModalOpen && (
        <div className="modal-overlay" onClick={() => !addIdentifierMutation.isPending && !updateIdentifierMutation.isPending && setIdentifierModalOpen(false)}>
          <div className="modal-card" onClick={(e) => e.stopPropagation()}>
            <h3>{editingIdentifierId != null ? 'Edit identifier' : 'Add identifier'}</h3>
            <form onSubmit={(e) => {
              e.preventDefault();
              setSectionError('');
              if (editingIdentifierId != null) {
                updateIdentifierMutation.mutate({ identifierId: editingIdentifierId, body: { documentKey: identifierKey || undefined, description: identifierDesc || undefined } });
              } else {
                const typeId = Number(identifierTypeId);
                if (!Number.isFinite(typeId) || typeId <= 0) { setSectionError('Select a type.'); return; }
                if (!identifierKey.trim()) { setSectionError('Enter key/number.'); return; }
                addIdentifierMutation.mutate({ documentTypeId: typeId, documentKey: identifierKey.trim(), description: identifierDesc.trim() || undefined });
              }
            }}>
              {!editingIdentifierId && (
                <label>
                  <span>Type</span>
                  <select value={identifierTypeId} onChange={(e) => setIdentifierTypeId(e.target.value)} required disabled={addIdentifierMutation.isPending}>
                    <option value="">Select type</option>
                    {identifierTypeOptions.map((t: { id: number; name?: string; value?: string }) => (
                      <option key={t.id} value={t.id}>{t.name ?? t.value ?? `Type ${t.id}`}</option>
                    ))}
                  </select>
                </label>
              )}
              <label>
                <span>Key / number</span>
                <input value={identifierKey} onChange={(e) => setIdentifierKey(e.target.value)} required disabled={addIdentifierMutation.isPending || updateIdentifierMutation.isPending} />
              </label>
              <label>
                <span>Description</span>
                <input value={identifierDesc} onChange={(e) => setIdentifierDesc(e.target.value)} disabled={addIdentifierMutation.isPending || updateIdentifierMutation.isPending} />
              </label>
              <div className="form-actions">
                <button type="button" className="btn-secondary" onClick={() => setIdentifierModalOpen(false)} disabled={addIdentifierMutation.isPending || updateIdentifierMutation.isPending}>Cancel</button>
                <button type="submit" className="btn-primary" disabled={addIdentifierMutation.isPending || updateIdentifierMutation.isPending}>
                  {editingIdentifierId != null ? 'Save' : 'Add'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
