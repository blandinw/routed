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
            ["Prod" "192.22.33.44"    443 80]]}
```

You'll need CoffeeScript to use the provided dispatcher:
```
npm install -g coffee-script
```

Finally, grab the latest release at [github.com/blandinw/routed/releases](https://github.com/blandinw/routed/releases).
Then, run:
```
path/to/routed.app/Contents/MacOS/node-webkit # sudo is needed for ports < 1024
```

## Config file

key | required | value | doc
----|----------|-------|----
:title | required | string | Title appearing in the menubar
:command | required | string | String to run the dispatcher process
:modes | required | vector | Each element is a vector describing a mode [name ip port redirect]
:ssl-cert | optional | string | Path to certificate to use with SSL
:ssl-key | optional | string | Path to key to use with SSL

## Dev

To run from source:
```bash
$ lein cljsbuild once dev # compile CLJS to JS
$ zip routed.nw package.json public/index.html public/js/cljs.js # create node-webkit app
$ /path/to/node-webkit.app/Contents/MacOS/node-webkit routed.nw # may need sudo if port < 1024
```

You can create your own dispatcher. Implement the following interface:
```
mydispatcher <ip> <port> <cert> <key> <redirect>
```

## License

Copyright © 2013 Willy Blandin

Distributed under the Eclipse Public License, the same as Clojure.
