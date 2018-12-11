# TV series title download

Generates tree style TV directories for Mede8er boxes.

Requires a config file called ".titler.cfg" in home containing your API keys for TMDB and TVDB; looks like this -

```
tmdb: your-api-key
tvdb: your-api-key
```

### Command line

```
id                          tv series id to download, database specific, tvdb slug is allowed
-db db                      set database, either tmdb or tvdb, defaults to tmdb
-lang de                    set language, defaults to en
-player mede8er             set output style, only mede8er supported
-merge db1 key1 db2 key2    download and merge, db1 as primary
-resize "directory"         resize artwork appropriately
-search term                search tmdb and tvdb
-seasons 1,4,5              specify seasons to download
-target /tv                 target directory, defaults to cwd
```

### Examples

```
titler -search Penny
titler 54671 -target /tv -seasons 2-3
titler -merge tvdb 1234 tmdb 2345 -season 4 -target /tv -lang fr
titler -db tvdb penny-dreadful
```

sbt assembly to build an executable JAR.
