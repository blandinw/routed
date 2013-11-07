http = require('http')
https = require('https')
fs = require('fs')

debug_mode = false
log = ->
  console.log.apply(console, arguments)
debug = ->
  return unless debug_mode
  log.apply(console, arguments)
inspect = (x) ->
  JSON.stringify(x, false, 2)
usage = ->
  log "usage: #{process.argv[0]} #{process.argv[1]} <host> <port> <cert> <key> <redirect>"

fmt_request = (opts) ->
  scheme = if opts.port == 443
    "HTTPS"
  else
    "HTTP"

  host = if opts.headers and opts.headers.host
    opts.headers.host
  else
    opts.hostname

  paren = if host == opts.hostname
    ""
  else
    " (#{opts.hostname})"

  "#{scheme} #{opts.method} #{host}#{opts.path}#{paren}"

is_ssl = (p) ->
  p in [443]

main = ->
  host          = process.argv[2]
  port          = process.argv[3] && parseInt(process.argv[3], 10)
  redirect_port = process.argv[6] && parseInt(process.argv[6], 10)

  if is_ssl(port) || is_ssl(redirect_port)
    cert = process.argv[4] && fs.readFileSync(process.argv[4])
    key  = process.argv[5] && fs.readFileSync(process.argv[5])
  else
    cert = null
    key  = null

  if not host
    usage()
    process.exit(1)

  if (is_ssl(port)) && !(cert && key)
    usage()
    process.exit(1)

  log "> Dispatching to #{host}:#{port}"

  forward_request = (req, resp) ->
    opts =
      method: req.method
      hostname: host
      port: port
      path: req.url
      headers: req.headers

    body = ''
    req.on 'data', (chunk) ->
      body += chunk.toString('binary')

    req.on 'end', ->
      log "> #{fmt_request(opts)}"

      new_req = https.request opts, (new_resp) ->
        resp.writeHead new_resp.statusCode, new_resp.headers
        new_resp.on 'data', (chunk) ->
          resp.write(chunk)
        new_resp.on 'end', ->
          resp.end()
        new_resp.on 'error', (err) ->
          log "> New resp error: #{err.message}"

      if body.length
        debug '> Writing body', body.length
        new_req.write(body, 'binary')

      new_req.on 'error', (err) ->
        log "> New req error: #{err.message}"
        resp.writeHead 503, {}
        resp.write("Dispatcher: #{err.message}")
        resp.end()

      new_req.end()
    req.on 'error', (e) ->
      log "> New resp error: #{err.message}"

  app = if is_ssl(port)
    https.createServer { key: key, cert: cert }, forward_request
  else
    http.createServer forward_request
  app.listen(port, '0.0.0.0')

  if redirect_port
    redirect_request = (req, resp) ->
      url = "https://#{req.headers.host}#{req.url}"
      log '> Redirecting to ' + url
      resp.writeHead 301, { 'Location': url }
      resp.end()

    redirect = if is_ssl(redirect_port)
      https.createServer { key: key, cert: cert }, redirect_request
    else
      http.createServer redirect_request
    redirect.listen(redirect_port, '0.0.0.0')

main()
