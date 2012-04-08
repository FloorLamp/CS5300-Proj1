Wences - session tables/read/write/update table
Ethan - RPC
Jasdeep - Cache
Norton - Group Membership

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

SimpleDB
Our SimpleDB hbas domain "CS5300PROJECT1BSDBMbrList" with one item "members" with attribute "ipps". The value is a single string of all members separated by an underscore.

RPC
-Format: 


Cache
- The cache is set up using both a linked list and a hash table
- The hash table is used for fast lookups, keyed by a string that matches
  the session id
- The linked list stores the keys present in the hash table in the order
  that they were placed into the hash table. We use this to determine how
  many sessions are present in the hash table and which one should be removed
  upon inserting a new session
- When placing a session in the table, we must scan through the list of sessions
  to check if it is already present.


Source files:
- groupMembership.GroupMembershipManager
  - 
- groupMembership.Server
  - 
- rpc.RPCClient
  - 
- rpc.RPCServer
- session.Session
- session.SessionCache
- session.SessionCleaner
- session.SessionManage
