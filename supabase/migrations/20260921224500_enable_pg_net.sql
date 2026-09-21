-- Enable pg_net so database functions can make asynchronous HTTP requests,
-- including calls to Supabase Edge Functions.

create extension if not exists pg_net
with schema extensions;