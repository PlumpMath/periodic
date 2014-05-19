# periodic [![Build Status](https://travis-ci.org/hadronzoo/periodic.svg)](https://travis-ci.org/hadronzoo/periodic)

Core.async channels that periodically supply values.

## Artifacts

`periodic` artifacts are
[released to clojars](https://clojars.org/com.joshuagriffith/periodic).

If you are using Maven, add the following repository definition to
your `pom.xml`:

```xml
<repository>
  <id>clojars.org</id>
  <url>http://releases.clojars.org/repo</url>
</repository>
```

Or with Leiningen:

```clj
[com.joshuagriffith/periodic "0.1.0"]
```

## Usage

### Periodic Source Channel

```clj
(periodic< [period in & options])
```

Takes a period and a source channel, and returns a channel which is
periodically supplied with values from the source channel. Returned
channel closes when source channel closes. Uses a PID controller to
adapt to blocking delays.

Options are passed as `:key val`. Supported options:

  - `:units val`: period unit (`:nHz`, `:µHz`, `:uHz`, `:mHz`, `:Hz`,
    `:kHz`, `:MHz`, `:GHz`, `:ns`, `:µs`, `:us`, `:ms`, `:s`, `:ks`,
    `:Ms`, `:Gs`); defaults to `:s`

  - `:kp val`: PID controller proportional gain; defaults to 1/100

  - `:ki val`: PID controller integral gain; defaults to 1/1000

  - `:kd val`: PID controller derivative gain; defaults to 1/10000

  - `:error val`: timing error channel

  - `:buf-or-n val`: buffer or buffer size for returned channel

  - `:period-error-limit`: reset error to zero when timing error
    exceeds this many periods. Useful for preventing overcorrection
    when source or target channels block for an extended time;
    defaults to 2 periods

### Periodic Target Channel

```clj
(periodic> [period out & options])
```

Like `periodic<`, but takes a period and a target channel and returns
a channel which periodically supplies values to the target
channel. Takes the same options as `periodic<`.

## License

Copyright © 2014 Joshua B. Griffith.

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
