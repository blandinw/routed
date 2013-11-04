# routed

A simple menubar to easily route the same request to a server or another.

## Usage

Create a config file (located at ~/.routedrc) like so (cf. below for details):
```clojure
{:title    "Status: "
 :command  "coffee path/to/dispatcher.coffee"
 :ssl-cert "path/to/cert.pem"  ;; only if using https/ssl
 :ssl-key  "path/to/mykey.key" ;; only if using https/ssl
 :modes    [["Dev"  "192.168.111.222" 443 80]
            ["Prod" "192.241.206.55"  443 80]]}
```

To use the provided dispatcher:
```
npm install -g coffee-script
# if specified ports are < 1024, will need to be started using sudo
```

To run routed from repo:
```
lein cljsbuild once dev
zip routed.nw package.json public/index.html public/js/cljs.js
/path/to/node-webkit.app/Contents/MacOS/node-webkit routed.nw # may need sudo if port < 1024
```

You can create your own dispatcher. Implement the following interface:
```
mydispatcher <ip> <port> <cert> <key> <redirect>
```

## Config file

key | required | value | doc
----|----------|-------|----
:title | required | string | Title appearing in the menubar
:command | required | string | String to run the dispatcher process
:modes | required | vector | Each element is a vector describing a mode [name ip port redirect]
:ssl-cert | optional | string | Path to certificate to use with SSL
:ssl-key | optional | string | Path to key to use with SSL


## License

Copyright © 2013 Willy Blandin

Distributed under the Eclipse Public License, the same as Clojure.
