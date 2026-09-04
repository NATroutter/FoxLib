![banner](https://cdn.nat.gg/img/foxlib_banner.png)

<div align="center">
<h1 style="margin: 0;font-weight: 700;font-family:-apple-system,BlinkMacSystemFont,Segoe UI,Helvetica,Arial,sans-serif,Apple Color Emoji,Segoe UI Emoji">FoxLib</h1>

![GitHub](https://img.shields.io/github/license/NATroutter/FoxBot?style=for-the-badge)
![Jenkins Build](https://img.shields.io/jenkins/build?jobUrl=https%3A%2F%2Fjenkins.nat.gg%2Fjob%2Fgeneral%2Fjob%2FFoxLib%2F&style=for-the-badge)
![ReposiliteVersion](https://img.shields.io/badge/dynamic/json?url=https%3A%2F%2Frepo.nat.gg%2Fapi%2Fmaven%2Flatest%2Fversion%2Freleases%2Ffi%2Fnatroutter%2Ffoxlib%2F&query=%24.version&style=for-the-badge&label=Version&color=%23329ea8)


FoxLib is NATroutter's common purposes java library that provides some useful features to make life easier

</div>

## Support
- [Discord](https://discord.nat.gg/)
- [Issue Tracker](https://github.com/NATroutter/FoxLib/issues)

## Quick Links
- [Jenkins](https://jenkins.nat.gg/job/general/job/FoxLib/)
- [Repository](https://repo.nat.gg/#/releases/fi/natroutter/foxlib)

## Maven
````xml
<repository>
	<id>natroutter-releases</id>
	<name>NATroutter's Repository</name>
	<url>https://repo.nat.gg/releases</url>
</repository>

<dependency>
    <groupId>fi.natroutter</groupId>
    <artifactId>foxlib</artifactId>
    <version>{VERSION}</version>
</dependency>
````

## 1.5.0 is a breaking change

The MongoDB driver, snakeyaml, gson and jsoup are `provided` from this version on, so they are
no longer pulled in for you. Declare the one you need beside FoxLib:

| If you use | Declare |
|---|---|
| `foxlib.mongo` | `org.mongodb:mongodb-driver-sync` |
| `foxlib.config.ConfigProvider` | `org.yaml:snakeyaml` |
| `foxlib.updates.GitHubVersionChecker` | `com.google.code.gson:gson` and `org.jsoup:jsoup` |

Nothing else changes on the dependency front. `FoxLogger`, `Cooldown`, the file helpers and the
statics on `FoxLib` need no dependency of their own.

Why: the published jar bundled all four, plus Lombok's runtime agent and installer, into every
consumer. A project that wanted a logger received an HTML parser, a YAML parser, a database
driver and a second copy of gson under the same package as its own, which decided at shade time
rather than at resolution time which one actually ran.

### What else a `FoxLogger` consumer will notice

- Timestamps now follow the configured time zone; `setTimeZone` previously had no effect. The
  default is the system zone, so nothing changes unless you set one.
- Log files roll to `.2.log`, `.3.log` and so on past 32 MB, where previously age was the only
  bound.
- The save thread is now a daemon, so a process that exits without calling `close()` loses up to
  one save interval of buffered lines. Call `close()` on shutdown.