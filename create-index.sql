

alter table yacm_user add index(username(16));

alter table yacm_userauth add index(userid);

alter table yacm_ugroup add index(groupname(16));


alter table yacm_content add index(path(32));
alter table yacm_content add index(state);
alter table yacm_content add index(create_date);
alter table yacm_content add index(modified_date);
alter table yacm_content add index(creator);
alter table yacm_content add index(last_modifier);


alter table yacm_content_history add index(content);
alter table yacm_content_history add index(user);
alter table yacm_content_history add index(version);
alter table yacm_content_history add index(modified_date);
alter table yacm_content_history add index(change_type);

alter table yacm_content_perms add index(content);
alter table yacm_content_perms add index(user);
alter table yacm_content_perms add index(ugroup);


alter table yacm_metadata add index(content);
alter table yacm_metadata add index(name(32));
alter table yacm_metadata add index(value(32));


alter table yacm_visit add index(action);
alter table yacm_visit add index(path(32));
alter table yacm_visit add index(user);
alter table yacm_visit add index(ip);
alter table yacm_visit add index(access_date);
alter table yacm_visit add index(error(32));
alter table yacm_visit add index(referrer(32));

