local data = redis.call('HMGET', KEYS[1], 'value', 'fetchedAtMs')
local value, fetchedAt = data[1], data[2]

if not value then
    local acquired = redis.call('SET', KEYS[2], ARGV[3], 'NX', 'PX', ARGV[4])
    if acquired then return {'LOAD', ''} end
    return {'WAIT', ''}
end

if tonumber(ARGV[1]) - tonumber(fetchedAt) < tonumber(ARGV[2]) then
    return {'HIT', value}
end

local acquired = redis.call('SET', KEYS[2], ARGV[3], 'NX', 'PX', ARGV[4])
if acquired then return {'STALE_REFRESH', value} end
return {'HIT', value}
