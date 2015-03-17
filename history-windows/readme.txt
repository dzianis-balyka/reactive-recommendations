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


актуальные проблемы
- фулл скан для фильтрации уже просмотренных курсов
- n фулл сканов для выбора курсов по n термам + непонятно как делать сортировку по релевантности, т.к. сначала надо зачитать всё в память
- как организовывать быстро индексы при появлении нового сочетания термов
- переключение юзера между кластерами пол+возраст+гео+термы
    - нужно атомарно поменять счётчики в кластерах - в новом прибавить в старом отнять
- подмешивание кластерных данных также потребует фулл скана по набору просмотренных курсов в рамках кластера

итого кластер будет постоянно нагружен сканами + локами изза переходов между кластерами

лучше хранить в hbase оконные данные а в эластике делать поиск с бустами и сортировками
