-- Shared markers are explicit, temporary, and visible only to accepted account friends.
create table public.rim_social_profiles(
 user_id uuid primary key references auth.users(id) on delete cascade,
 friend_code text unique not null default upper(encode(extensions.gen_random_bytes(6),'hex')),
 nickname text not null default '', player_uuid text not null default '',
 server_hash text not null default '', dimension text not null default '',
 online boolean not null default false,last_seen timestamptz not null default now()
);
create table public.rim_friendships(
 sender uuid not null references auth.users(id) on delete cascade,
 recipient uuid not null references auth.users(id) on delete cascade,
 accepted boolean not null default false,created_at timestamptz not null default now(),
 primary key(sender,recipient),check(sender<>recipient)
);
create unique index rim_friend_pair on public.rim_friendships(least(sender,recipient),greatest(sender,recipient));
create index rim_friend_recipient on public.rim_friendships(recipient);
create table public.rim_friend_markers(
 id uuid primary key default gen_random_uuid(),owner_id uuid not null references auth.users(id) on delete cascade,
 server_hash text not null,dimension text not null,name text not null,
 x double precision not null,y double precision not null,z double precision not null,
 created_at timestamptz not null default now(),expires_at timestamptz not null default now()+interval '60 seconds'
);
create index rim_marker_owner on public.rim_friend_markers(owner_id,expires_at);
create index rim_marker_expiry on public.rim_friend_markers(expires_at);
alter table public.rim_social_profiles enable row level security;
alter table public.rim_friendships enable row level security;
alter table public.rim_friend_markers enable row level security;
revoke all on public.rim_social_profiles,public.rim_friendships,public.rim_friend_markers from public,anon,authenticated;

create function public.mod_social(p_token text,p_action text,p_payload jsonb default '{}'::jsonb) returns jsonb
language plpgsql security definer set search_path='' as $$
declare u uuid; target uuid; mine public.rim_social_profiles; other public.rim_social_profiles;
 srv text; dim text; nick text; puuid text; notice text:=''; x double precision; y double precision; z double precision;
 fs jsonb; incoming jsonb; sent jsonb; markers jsonb;
begin
 if p_token is null or p_token!~'^[a-f0-9]{64}$' then return jsonb_build_object('status','unauthorized');end if;
 select s.user_id into u from public.mod_sessions s where s.token_hash=encode(extensions.digest(p_token,'sha256'),'hex') and s.expires_at>now();
 if u is null or public.mod_access_until(u) is null then return jsonb_build_object('status','unauthorized');end if;
 if p_action is null or p_action not in ('poll','request','accept','remove','ping') or p_payload is null or jsonb_typeof(p_payload)<>'object' or octet_length(p_payload::text)>2048 then return jsonb_build_object('status','invalid');end if;
 if not public.mod_rate('social:'||u,90) then return jsonb_build_object('status','rate_limited');end if;
 -- Serializes friend limits and marker writes from the same account.
 perform pg_advisory_xact_lock(hashtextextended(u::text,0));
 srv:=coalesce(p_payload->>'server','');dim:=coalesce(p_payload->>'dimension','');nick:=coalesce(p_payload->>'nickname','');puuid:=coalesce(p_payload->>'player_uuid','');
 if srv<>'' and srv!~'^[a-f0-9]{64}$' then return jsonb_build_object('status','invalid');end if;
 if length(dim)>80 or (dim<>'' and dim!~'^[a-z0-9_.-]+:[a-z0-9_/.-]+$') then return jsonb_build_object('status','invalid');end if;
 if nick!~'^[A-Za-z0-9_]{1,16}$' or (puuid<>'' and puuid!~'^[a-fA-F0-9-]{36}$') then return jsonb_build_object('status','invalid');end if;
 insert into public.rim_social_profiles(user_id,nickname,player_uuid,server_hash,dimension,online)
 values(u,nick,puuid,srv,dim,coalesce((p_payload->>'online')::boolean,false) and srv<>'' and dim<>'' and puuid<>'')
 on conflict(user_id) do update set nickname=excluded.nickname,player_uuid=excluded.player_uuid,server_hash=excluded.server_hash,dimension=excluded.dimension,online=excluded.online,last_seen=now();
 select * into mine from public.rim_social_profiles where user_id=u;
 if p_action in ('request','accept','remove') then
  perform pg_advisory_xact_lock(hashtextextended('rim-social-friend-writes',0));
  if not public.mod_rate('social-friend:'||u,20) then return jsonb_build_object('status','rate_limited');end if;
  select * into other from public.rim_social_profiles where friend_code=upper(coalesce(p_payload->>'code',''));
  target:=other.user_id;
  if target is null or target=u then return jsonb_build_object('status','invalid_friend_code');end if;
  if p_action='request' then
   if (select count(*) from public.rim_friendships where sender=u or recipient=u)>=50 or (select count(*) from public.rim_friendships where sender=target or recipient=target)>=50 then return jsonb_build_object('status','friend_limit');end if;
   insert into public.rim_friendships(sender,recipient) values(u,target) on conflict do nothing;notice:='Запрос отправлен';
  elsif p_action='accept' then
   update public.rim_friendships set accepted=true where sender=target and recipient=u;
   if not found then return jsonb_build_object('status','no_incoming_request');end if;notice:='Друг добавлен';
  else
   delete from public.rim_friendships where (sender=u and recipient=target) or (sender=target and recipient=u);notice:='Связь удалена';
  end if;
 end if;
 delete from public.rim_friend_markers where expires_at<=now();
 if p_action='ping' then
  if srv='' or dim='' then return jsonb_build_object('status','server_required');end if;
  if not public.mod_rate('social-ping:'||u,30) then return jsonb_build_object('status','rate_limited');end if;
  x:=(p_payload->>'x')::double precision;y:=(p_payload->>'y')::double precision;z:=(p_payload->>'z')::double precision;
  if x is null or y is null or z is null or abs(x)>30000000 or abs(z)>30000000 or abs(y)>4096 then return jsonb_build_object('status','invalid_coordinates');end if;
  -- Keep at most five recent pings for each account.
  delete from public.rim_friend_markers where owner_id=u and id not in (select id from public.rim_friend_markers where owner_id=u order by created_at desc limit 4);
  insert into public.rim_friend_markers(owner_id,server_hash,dimension,name,x,y,z) values(u,srv,dim,left(coalesce(nullif(p_payload->>'name',''),'Метка'),40),x,y,z);notice:='Метка отправлена друзьям на 60 секунд';
 end if;
 select coalesce(jsonb_agg(jsonb_build_object('code',p.friend_code,'nickname',p.nickname,'kind','friend',
  'online',p.online and p.last_seen>now()-interval '35 seconds' and public.mod_access_until(p.user_id) is not null and exists(select 1 from public.mod_sessions s where s.user_id=p.user_id and s.expires_at>now()),
  'here',p.online and p.last_seen>now()-interval '35 seconds' and p.server_hash=srv and p.dimension=dim and public.mod_access_until(p.user_id) is not null and exists(select 1 from public.mod_sessions s where s.user_id=p.user_id and s.expires_at>now()),
  'player_uuid',case when p.online and p.last_seen>now()-interval '35 seconds' and p.server_hash=srv and p.dimension=dim then p.player_uuid else '' end)),'[]'::jsonb)
 into fs from public.rim_social_profiles p where exists(select 1 from public.rim_friendships f where f.accepted and ((f.sender=u and f.recipient=p.user_id) or (f.recipient=u and f.sender=p.user_id)));
 select coalesce(jsonb_agg(jsonb_build_object('code',p.friend_code,'nickname',p.nickname,'kind','incoming')),'[]'::jsonb)
 into incoming from public.rim_social_profiles p join public.rim_friendships f on f.sender=p.user_id where f.recipient=u and not f.accepted;
 select coalesce(jsonb_agg(jsonb_build_object('code',p.friend_code,'nickname',p.nickname,'kind','sent')),'[]'::jsonb)
 into sent from public.rim_social_profiles p join public.rim_friendships f on f.recipient=p.user_id where f.sender=u and not f.accepted;
 select coalesce(jsonb_agg(jsonb_build_object('id',m.id,'nickname',p.nickname,'name',m.name,'x',m.x,'y',m.y,'z',m.z,'expires_at',m.expires_at)),'[]'::jsonb)
 into markers from (
  select mm.* from public.rim_friend_markers mm
  where mm.server_hash=srv and mm.dimension=dim and mm.expires_at>now()
  and (mm.owner_id=u or exists(select 1 from public.rim_friendships ff where ff.accepted and ((ff.sender=u and ff.recipient=mm.owner_id) or (ff.recipient=u and ff.sender=mm.owner_id))))
  order by mm.created_at desc limit 32
 ) m join public.rim_social_profiles p on p.user_id=m.owner_id
 where m.server_hash=srv and m.dimension=dim and m.expires_at>now() and public.mod_access_until(m.owner_id) is not null
 and exists(select 1 from public.mod_sessions s where s.user_id=m.owner_id and s.expires_at>now())
 and (m.owner_id=u or exists(select 1 from public.rim_friendships f where f.accepted and ((f.sender=u and f.recipient=m.owner_id) or (f.recipient=u and f.sender=m.owner_id))));
 return jsonb_build_object('status','ok','code',mine.friend_code,'note',notice,'friends',fs,'incoming',incoming,'sent',sent,'markers',markers);
exception when invalid_text_representation or numeric_value_out_of_range then return jsonb_build_object('status','invalid');
end $$;
revoke all on function public.mod_social(text,text,jsonb) from public;
grant execute on function public.mod_social(text,text,jsonb) to anon,authenticated;

