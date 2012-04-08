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

