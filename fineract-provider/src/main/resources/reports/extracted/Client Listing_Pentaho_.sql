-- report: Client Listing(Pentaho)
-- parameters: tenantUrl (java.lang.String), username (java.lang.String), password (java.lang.String), userhierarchy (java.lang.String)

-- query: Select_Office
select mo.id, mo.name
from m_office mo
order by id

-- query: base
select 
concat(repeat("..",   
   ((LENGTH(mounder.`hierarchy`) - LENGTH(REPLACE(mounder.`hierarchy`, '.', '')) - 1))), mounder.`name`) as "Office/Branch",
	mounder.name, mounder.id,
 mc.account_no as "Client Account No.",  
mc.display_name as "Name",  
rev.enum_message_property as "Status",
mc.activation_date as "Activation", ifnull(mc.external_id,"-") as "External Id"
from m_office mo
join m_office mounder on mounder.hierarchy like concat(mo.hierarchy, '%')
and mounder.hierarchy like concat(${userhierarchy}, '%')
join m_client mc on mc.office_id = mounder.id
left join r_enum_value rev on rev.enum_name = 'status_enum' and rev.enum_id = mc.status_enum
where mo.id=${selectOffice}
order by mounder.hierarchy, mc.account_no

