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


RPCClient and RPCServer

RPCClient contains the SessionRead, SessionWrite, SessionDelete, and Noop functions. The messages, defined by a UUID- generated callID and a corresponding operation code, are marshalled into byte[] and sent as UDP packet. RPCClient waits for a response and unmarshals the packet. Noop returns successfully, sessionRead returns session data, and sessionWrite/sessionDelete return successfully. 

The RPC Server loops continuously and processes requests based on the operation code. It writes a session for sessionWrite (along with the discard time), retreivs a session for sessionWrite, deletes a session for sessionDelete, and simply returns a callID packet for Noop to acknowledge the request. RPC messages are a string marshalled into a byte[], with callID, operation code, and other data separated by underscores ("_").

=======
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
>>>>>>> 3db12d470f10f9a73539eb2b669c61f493ecd3d1
