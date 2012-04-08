Wences - session tables/read/write/update table
Ethan - RPC
Jasdeep - Cache
Norton - Group Membership - mostly done, need RPC signatures

Server
Each server will have an IP number and port number.

Session Manager
-Contains a concurrent hashtable with cookie values as the key and a class called sessions
as the value. The session clas contains the ID, message, expiration, date, primary and
backup servers.

Session functions
-Get and increment: It will update the message and version count.
-Create cookies: Send out the cookie with new value.
-Destroy cookies: Delete the cookie and send it out to expire.
-There are functions which take care of reading and writing. Each one of them
has a lock to make sure there are no conflicts with garbage collect.

Garbage collect functions
-The garbage collect function will have a periodic time and run periodically. Every
time it runs, it will obtain the lock and lock the table until it is done. Then there 
be no conflicts between updating, writing and garbage collecting.