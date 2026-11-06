-- report: Active Loan Summary per Branch
-- parameters: tenantUrl (java.lang.String), username (java.lang.String), password (java.lang.String), userhierarchy (java.lang.String), userid (java.lang.Long)

-- query: Query 1
select a.name Branch,f.clients,a. Disbursed,a.principalpaid,a.intrestpaid,a. principalout,a.intrestout,
 a.Amountarres,b.groupcount,c.centercount,d.activeloan,e.areesloan 'no of loan arres'
 from
 (select ounder.name,
 
 sum(ml.principal_disbursed_derived) Disbursed,
 ounder.id oid,
 sum(ml.principal_repaid_derived) principalpaid,
 sum(ml.interest_repaid_derived) intrestpaid,
 sum(ml.principal_outstanding_derived) principalout,
sum(ml.interest_outstanding_derived) intrestout,
ifnull(sum( mlarr.total_overdue_derived),0) Amountarres

from m_office o
left join m_office ounder on ounder.hierarchy like concat(o.hierarchy,'%')
 and ounder.hierarchy like concat(${userhierarchy},'%')
left join m_client mc on mc.office_id=ounder.id
left join m_loan ml
on ml.client_id=mc.id																		
left join m_loan_arrears_aging mlarr
on mlarr.loan_id=ml.id
where mc.status_enum=300 and o.id=1
group by  ounder.name) a

inner join (select '#group',count(mg2.id) as groupcount,ounder.id oid
from m_office o
left join m_office ounder on ounder.hierarchy like concat(o.hierarchy,'%')
and ounder.hierarchy like concat(${userhierarchy},'%')

left join m_group mg2 on mg2.office_id=ounder.id and mg2.level_id=2
#where mg2.level_id=2
where o.id=1
group by  ounder.id) b on b.oid=a.oid
inner join (select count(mg.id) as centercount,ounder.id oid
from m_office o
left join m_office ounder on ounder.hierarchy like concat(o.hierarchy,'%')
and ounder.hierarchy like concat(${userhierarchy},'%')

left join m_group mg on mg.office_id=ounder.id and mg.level_id=1
#where mg.level_id=1
where o.id=1
group by  ounder.id) c on b.oid=c.oid
inner join (select count(ml.id) activeloan,ounder.id oid
from m_office o
left join m_office ounder on ounder.hierarchy like concat(o.hierarchy,'%')
and ounder.hierarchy like concat(${userhierarchy},'%')
inner join m_client mc on mc.office_id=ounder.id
inner join m_loan ml on ml.client_id=mc.id
where ml.loan_status_id=300 and o.id=1
group by  ounder.name
)d on c.oid=d.oid
left join (select count(mlarr.loan_id) areesloan, ounder.id oid
from m_office o
left join m_office ounder on ounder.hierarchy like concat(o.hierarchy,'%')
and ounder.hierarchy like concat(${userhierarchy},'%')
left join m_client mc on mc.office_id=ounder.id
left join m_loan ml on ml.client_id=mc.id
left join m_loan_arrears_aging mlarr on ml.id=mlarr.loan_id
where ml.loan_status_id=300
 and o.id=1
group by ounder.name)e on c.oid=e.oid            

inner join (select count(mc.id) clients, ounder.id oid
from m_office o
join m_office ounder on ounder.hierarchy like concat(o.hierarchy,'%')
and ounder.hierarchy like concat(${userhierarchy},'%')
inner join m_client mc on mc.office_id=ounder.id 
where mc.status_enum=300 and o.id=1
group by ounder.name )f on b.oid=f.oid

-- query: user
select concat(firstname,' ',lastname) name from m_appuser
where id=${userid}

