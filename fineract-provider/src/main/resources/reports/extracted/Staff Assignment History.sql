-- report: Staff Assignment History
-- parameters: tenantUrl (java.lang.String), username (java.lang.String), password (java.lang.String), centerId (java.lang.Long)

-- query: date 
select mgc.id center_id
,mgc.display_name center_name
,msah.staff_id staff_id
,ms.display_name staff_name
, msah.start_date from_date
,msah.end_date
,msah.created_date 
,if(msah.end_date!=msah.created_date,msah.lastmodified_date,'') AS dadate
,msah.createdby_id 
,concat(u.firstname,' ',u.lastname) created_by
,if(msah.end_date!=msah.created_date,concat(ul.firstname,' ',ul.lastname),'') AS last_modified_by



from m_staff_assignment_history msah

inner join m_group mgc on mgc.id=msah.centre_id and mgc.level_id=1
inner join m_staff ms on ms.id=msah.staff_id
inner join m_appuser u on u.id=msah.createdby_id
inner join m_appuser ul on ul.id=msah.lastmodifiedby_id
where mgc.id=${centerId}

