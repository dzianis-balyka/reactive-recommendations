hbase table for interval data

disable 'rr_interval_data'
drop 'rr_interval_data'
create 'rr_interval_data',
    {NAME=>'h', VERSIONS=>1, TTL=>86400, COMPRESSION=>'SNAPPY'},
    {NAME=>'d', VERSIONS=>1, TTL=>1209600, COMPRESSION=>'SNAPPY'},
    {NAME=>'w', VERSIONS=>1, TTL=>5184000, COMPRESSION=>'SNAPPY'},
    {NAME=>'m', VERSIONS=>1, TTL=>31622400, COMPRESSION=>'SNAPPY'},
    {NAME=>'t', VERSIONS=>1, COMPRESSION=>'SNAPPY'}
    {NAME=>'a', VERSIONS=>20, COMPRESSION=>'SNAPPY'}

create 'rr_users',
    {NAME=>'d', VERSIONS=>1, COMPRESSION=>'SNAPPY'}

create 'rr_content',
    {NAME=>'d', VERSIONS=>1, COMPRESSION=>'SNAPPY'}