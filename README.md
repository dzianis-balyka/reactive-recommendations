reactive-recommendations
========================

TODO
- performance tests
- try parent/child for speed optimizations
- add windows
- add caching (or batch calculations) for users (categories,tags,items) history
- add "find similar users by actions"


to work from windows - open ports
netsh advfirewall firewall add rule name=AllowRPCCommunication dir=in action=allow protocol=TCP localport=49152-65535