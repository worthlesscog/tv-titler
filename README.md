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
-merge db1 key1 db2 key2    download and merge, db1 as primary
-player mede8er             set output style, only mede8er supported
-resize "directory"         resize artwork appropriately
-search term                search database
-searchall term             search all databases, merge results by name/air date 
-seasons 1,4,5              specify seasons to download
-target /tv                 target directory, defaults to cwd
```

### Examples

```
titler -search Penny
titler 54671 -target /tv -seasons 2-3
titler -merge tvdb 1234 tmdb 2345 -season 4 -target /tv -lang fr
titler -db tvdb penny-dreadful
titler -searchall Battlestar
```

### Search output 

```
                                         Genre   Lang   Aired        tmdb    tvdb     Name
                                                        2007-03-01           207131   BSGCast
                        Drama, Science Fiction   en     1978-09-17   501     71173    Battlestar Galactica
                     Action & Adventure, Drama   en     2003-12-08   71365            Battlestar Galactica
   Action & Adventure, Drama, Sci-Fi & Fantasy   en     2004-10-18   1972             Battlestar Galactica
                                                        2003-12-08           73545    Battlestar Galactica (2003)
                              Sci-Fi & Fantasy   en     2012-11-09   33240   204781   Battlestar Galactica: Blood & Chrome
                       Drama, Sci-Fi & Fantasy   en     2007-10-05   61910            Battlestar Galactica: Razor Flashbacks
                        Science Fiction, Drama   en     2008-12-12   8546             Battlestar Galactica: The Face of the Enemy
      Science Fiction, Drama, Sci-Fi & Fantasy   en     2006-09-05   4882             Battlestar Galactica: The Resistance
                                                 en     1981-10-26   12090            Battlestars
                              Sci-Fi & Fantasy   en     1980-01-27   4621    71170    Galactica 1980
```
sbt assembly to build an executable JAR.
