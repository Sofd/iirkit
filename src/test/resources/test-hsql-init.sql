-- the create table statements (should) work every time, even if the text files already exist. Not sure why.

-- NEVER use double quotes, always use single quotes in here. Spring's SQL parser does not
-- recognize double quotes as quoting characters and thus will parse semicolons inside
-- double-quoted strings as line delimiters...

CREATE TABLE user(name varchar(20), password varchar(20), roles varchar(100000));

CREATE TABLE iircase(userName varchar(20), caseNr integer, hangingProtocol varchar(10000), result varchar(100000));
