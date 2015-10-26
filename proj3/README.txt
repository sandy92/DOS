Chord
-----

Input: 'numNodes' and 'numRequests' where numNodes is size of the chord ring that needs to be created and numRequests is number of requests that needs to be searched  in the ring. 

Output: Average number of hops per Request.

Working Part:

A chord ring is created with the given number of 'numNodes' in the input.
To simulate the lookup functionality, we are dynamically generating messages which will be stored in the chord ring. In our project, the number of messages that are generated is 5*numRequests. From this existing message pool, we randomly choose 'numRequests' number of messages and perform the lookup on these message to calculate the average hop count per message(aka per Request)

Largest Node Network:

Due to time limitations, we were only able to test the creation of a network of size 250 nodes. Below are few test results. 

numNodes	numRequests     Average Hops

    3 			2 				2
    13			6 				3
    13 			10 				2
    25 			15 				3

Approach followed during Implementation:

Every node is represented by an actor. We used first 31 bits of SHA-1 hash of the node name/path to determine the position of any given node in the network. The maximum size of network is 2^31 nodes. The Chord Ring network is initiated with a single node. Every new join operation is performed with the help of an existing node in the ring.

Utility functions:

The function 'printChordRing' takes n as an input argument, where n is a valid ring node(n=ActorRef), prints the chord ring starting from n in sequential order.


How to run the program 
---------------------------------------------------------------------

Download the folder Chord on local machine.

Go to the folder Chord from command prompt. 

sbt “run numnodes numrequests”
where numnodes is number of nodes that needs to be created in the system and numrequests is number of requests that should be performed by the nodes in the system.

Ex: sbt "run 10 20"

If number of requests is zero, then the program prints all the nodes in the chord ring in a sequential manner.
Ex: sbt "run 10 0".

Observations:
We noticed that the average hop count is close to the log2(n), where n is the number of nodes in the chord ring.


References:

1. https://pdos.csail.mit.edu/papers/chord:sigcomm01/chord_sigcomm.pdf
2. http://alvinalexander.com
3. https://www.safaribooksonline.com/library/view/scala-cookbook/9781449340292/
4. http://stackoverflow.com
5. http://zakvdm.org/blog/2013/02/07/generating-random-strings-in-scala/
6. http://danielwestheide.com/scala/neophytes.html
