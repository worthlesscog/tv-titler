# TV series title download

Generates tree style TV directories for Mede8er boxes.

Requires a config file called ".titler.cfg" in home containing your API keys; looks like this -

```
maze: dontcare
omdb: your-omdb-api-key
tmdb: your-tmdb-api-key
tvdb: your-tvdb-api-key
```
### Command line
```
-<db> key                   specify id to download for db, tvdb slug is allowed
-lang de                    set language, defaults to en
-player mede8er             set output style, only mede8er supported
-resize "directory"         resize artwork appropriately
-search term                search databases, merge results by name/air date 
-seasons 1,4,5              specify seasons to download
-target /tv                 target directory, defaults to cwd
```
### Examples
```
titler -search Penny
titler -tmdb 54671 -target /tv -seasons 2-3 -lang fr
titler -tvdb penny-dreadful -season 1
titler -search Battlestar
titler -tmdb 501 -maze 1059 -tvdb 71173
```
### Search output 
```
Searching TV Maze, The Open Movie Database, The Movie Database, The TVDB ...
                                         Genre   Lang      Aired        maze    omdb        tmdb    tvdb     Name
            Action, Adventure, Science-Fiction   English   1978-09-17   1059                501     71173    Battlestar Galactica
                                                           1978–1979            tt0076984                    Battlestar Galactica
                                                           2003                 tt0314979                    Battlestar Galactica
                     Action & Adventure, Drama   en        2003-12-08                       71365            Battlestar Galactica
   Action & Adventure, Drama, Sci-Fi & Fantasy   en        2004-10-18                       1972             Battlestar Galactica
                                                           2004–2009            tt0407362                    Battlestar Galactica
                   Drama, Science-Fiction, War   English   2005-01-14   166                                  Battlestar Galactica
                                                           2003-12-08                               73545    Battlestar Galactica (2003)
                Drama, Action, Science-Fiction   English   2012-11-09   870                 33240   204781   Battlestar Galactica: Blood & Chrome
                       Drama, Sci-Fi & Fantasy   English   2007-10-05   26696               61910            Battlestar Galactica: Razor Flashbacks
                                                           2007–                tt1334430                    Battlestar Galactica: Razor Flashbacks
                        Drama, Science-Fiction   English   2008-12-12   26697               8546             Battlestar Galactica: The Face of the Enemy
                                                           2008–                tt1338724                    Battlestar Galactica: The Face of the Enemy
      Science Fiction, Drama, Sci-Fi & Fantasy   English   2006-09-05   26692               4882             Battlestar Galactica: The Resistance
                                                           2006–                tt0840800                    Battlestar Galactica: The Resistance
                                                 en        1981-10-26                       12090            Battlestars
                                                           2007-03-01                               207131   BSGCast
                              Sci-Fi & Fantasy   en        1980-01-27                       4621    71170    Galactica 1980
Done
```
### Reference
```
OMDb API        https://www.omdbapi.com/
TMDb API v3     https://developers.themoviedb.org/3
TVDB API v2     https://api.thetvdb.com/swagger
TVmaze API      https://www.tvmaze.com/api
```
sbt assembly to build an executable JAR.
