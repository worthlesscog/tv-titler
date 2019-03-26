package com.worthlesscog.tv.text

import java.io.File

import com.worthlesscog.tv._
import com.worthlesscog.tv.data.{Credentials, Role, Token, TvDatabase, TvEpisode, TvSeason, TvSeries}

import scala.io.Source

class Text extends TvDatabase {

    val IMDB = home.resolve("imdb")

    val EPISODES = IMDB.resolve("title-episode.tsv").toFile
    val JOBS = IMDB.resolve("title-principals.tsv").toFile
    val PEOPLE = IMDB.resolve("name-basics.tsv").toFile
    val RATINGS = IMDB.resolve("title-ratings.tsv").toFile
    val TITLES = IMDB.resolve("title-basics.tsv").toFile

    val castMember = Set("actor", "actress")

    def databaseId = "text"

    def databaseName = "IMDB Text"

    def authenticate(c: Credentials) =
        TextToken() |> asRight

    def search(name: String, t: Token, lang: String) =
        s"$databaseId: search not implemented" |> asLeft

    def getTvSeries(identifier: String, seasonNumbers: Option[Set[Int]], t: Token, lang: String) =
        for {
            episodes <- map(EPISODES, 1, Set(identifier), episode)
            ids = episodes.keySet + identifier
            titles <- map(TITLES, 0, ids, title)
            ratings <- map(RATINGS, 0, ids, rating)
            jobs <- list(JOBS, 0, ids, job)
            people <- map(PEOPLE, 0, jobs map { _ person } toSet, person)
            requiredSeasons = seasonNumbers.getOrElse(1 to 99 toSet).toSeq.sorted
            availableSeasons = episodes.values.map(_ season).toSet
            seasons = requiredSeasons filter { availableSeasons contains }
            series <- buildTvSeries(identifier, episodes, titles, ratings, jobs, people, seasons)
        } yield series

    def map[T](file: File, field: Int, ids: Set[String], f: Array[String] => Option[T]) =
        maybeIO {
            ((for {
                l <- tsv(file)
                if ids contains l(field)
                t <- f(l)
            } yield l(0) -> t) toMap) |> asRight
        }

    // XXX - unclosed, but short lived
    def tsv(file: File) =
        Source.fromFile(file).getLines map { _ split '\t' }

    // title.episode.tsv.gz – Contains the tv episode information. Fields include:
    //     tconst (string) - alphanumeric identifier of episode
    //     parentTconst (string) - alphanumeric identifier of the parent TV Series
    //     seasonNumber (integer) – season number the episode belongs to
    //     episodeNumber (integer) – episode number of the tconst in the TV series
    //
    // ---------- TITLE-EPISODE.TSV
    // tt0682589       tt0063946       1       2
    def episode(fields: Array[String]) =
        Some(Episode(fields(0), fields(2) toInt, fields(3) toInt))

    // title.basics.tsv.gz - Contains the following information for titles:
    //     tconst (string) - alphanumeric unique identifier of the title
    //     titleType (string) – the type/format of the title (e.g. movie, short, tvseries, tvepisode, video, etc)
    //     primaryTitle (string) – the more popular title / the title used by the filmmakers on promotional materials at the point of release
    //     originalTitle (string) - original title, in the original language
    //     isAdult (boolean) - 0: non-adult title; 1: adult title
    //     startYear (YYYY) – represents the release year of a title. In the case of TV Series, it is the series start year
    //     endYear (YYYY) – TV Series end year. ‘\N’ for all other title types
    //     runtimeMinutes – primary runtime of the title, in minutes
    //     genres (string array) – includes up to three genres associated with the title
    //
    // ---------- TITLE-BASICS.TSV
    // tt0063946       tvSeries        My Partner the Ghost    Randall and Hopkirk (Deceased)  0       1969    1971    60      Action,Comedy,Crime
    // tt0682604       tvEpisode       The Ghost Who Saved the Bank at Monte Carlo     The Ghost Who Saved the Bank at Monte Carlo     0       1969    \N      52      Action,Comedy,Crime
    def title(fields: Array[String]) =
        fields(1) match {
            case "tvEpisode" | "tvSeries" => Some(Title(fields(2), maybeInt(fields(7)), fields(8) split ','))
            case _                        => None
        }

    def maybeInt(s: String) =
        s match {
            case "\\N" => 0
            case _     => s toInt
        }

    // title.ratings.tsv.gz – Contains the IMDb rating and votes information for titles
    //     tconst (string) - alphanumeric unique identifier of the title
    //     averageRating – weighted average of all the individual user ratings
    //     numVotes - number of votes the title has received
    //
    // ---------- TITLE-RATINGS.TSV
    // tt0063946       7.7     942
    def rating(fields: Array[String]) =
        Some(Rating(fields(1) toDouble, fields(2) toInt))

    def list[T](file: File, field: Int, ids: Set[String], f: Array[String] => T) =
        maybeIO {
            ((for {
                l <- tsv(file)
                if ids contains l(field)
            } yield f(l)) toList) |> asRight
        }

    // title.principals.tsv.gz – Contains the principal cast/crew for titles
    //     tconst (string) - alphanumeric unique identifier of the title
    //     ordering (integer) – a number to uniquely identify rows for a given titleId
    //     nconst (string) - alphanumeric unique identifier of the name/person
    //     category (string) - the category of job that person was in
    //     job (string) - the specific job title if applicable, else '\N'
    //     characters (string) - the name of the character played if applicable, else '\N'
    //
    // ---------- TITLE-PRINCIPALS.TSV
    // tt0063946       1       nm0695511       actor   \N      ["Jeff Randall"]
    // tt0063946       3       nm0028130       actress \N      ["Jeannie Hopkirk"]
    // tt0063946       5       nm0819388       writer  creator \N
    // tt0682604       10      nm0255830       cinematographer director of photography \N
    // tt0682604       1       nm0695511       actor   \N      ["Jeff Randall"]
    // tt0682604       3       nm0028130       actress \N      ["Jeannie Hopkirk"]
    def job(fields: Array[String]) =
        Job(fields(0), fields(1) toInt, fields(2), fields(3), fields(4), fields(5) split ',' map unquote)

    def unquote(s: String) =
        s filter ('"' !=)

    // name.basics.tsv.gz – Contains the following information for names:
    //     nconst (string) - alphanumeric unique identifier of the name/person
    //     primaryName (string)– name by which the person is most often credited
    //     birthYear – in YYYY format
    //     deathYear – in YYYY format if applicable, else '\N'
    //     primaryProfession (array of strings)– the top-3 professions of the person
    //     knownForTitles (array of tconsts) – titles the person is known for
    //
    // ---------- NAME-BASICS.TSV
    // nm0014952       Philip Aizlewood        \N      \N      editorial_department,production_manager,producer        tt0063946,tt0055701,tt0062551,tt0063893
    // nm0028130       Annette Andre   1939    \N      actress,soundtrack      tt0068054,tt0063946,tt0060438,tt0054518
    // nm0178560       Kenneth Cope    1931    \N      actor,writer,music_department   tt0066895,tt0056751,tt0272416,tt0063946
    // nm0695511       Mike Pratt      1931    1976    actor,soundtrack,writer tt0063946,tt0069273,tt0423055,tt0059646
    def person(fields: Array[String]) =
        Some(Person(fields(1)))

    private def buildTvSeries(id: String, episodes: Map[String, Episode], titles: Map[String, Title], ratings: Map[String, Rating], jobs: List[Job], people: Map[String, Person], seasons: Seq[Int]) = {
        val js = jobs groupBy { _ title }
        val t = titles get id
        val r = ratings get id

        TvSeries(
            airDate = None,
            backdropUrl = None,
            genres = t map { _ genres },
            language = None,
            name = t map { _ title },
            numberOfSeasons = None,
            overview = None,
            posterUrl = None,
            rated = None,
            rating = r map { _ rating },
            runtime = t map { _ runtime },
            seasons = Some(buildTvSeasons(id, episodes, titles, ratings, js, people, seasons)),
            status = None,
            votes = r map { _ count }) |> asRight
    }

    def buildTvSeasons(id: String, episodes: Map[String, Episode], titles: Map[String, Title], ratings: Map[String, Rating], jobs: Map[String, List[Job]], people: Map[String, Person], seasons: Seq[Int]) =
        seasons map buildTvSeason(id, episodes, titles, ratings, jobs, people)

    def buildTvSeason(id: String, episodes: Map[String, Episode], titles: Map[String, Title], ratings: Map[String, Rating], jobs: Map[String, List[Job]], people: Map[String, Person])(season: Int) = {
        val es = episodes.values.filter { _.season == season }.toList sortBy { _ episode }

        TvSeason(
            airDate = None,
            episodes = Some(buildTvEpisodes(es, titles, ratings, jobs, people)),
            number = Some(season),
            numberOfEpisodes = None,
            overview = None,
            posterUrl = None)
    }

    def buildTvEpisodes(es: List[Episode], titles: Map[String, Title], ratings: Map[String, Rating], jobs: Map[String, List[Job]], people: Map[String, Person]) =
        es map buildTvEpisode(titles, ratings, jobs, people)

    def buildTvEpisode(titles: Map[String, Title], ratings: Map[String, Rating], jobs: Map[String, List[Job]], people: Map[String, Person])(e: Episode) = {
        val r = ratings get e.id
        val t = titles get e.id

        TvEpisode(
            airDate = None,
            cast = jobs get e.id map cast(people),
            crew = None,
            name = t map { _ title },
            number = Some(e episode),
            overview = None,
            screenshotUrl = None,
            rating = r map { _ rating },
            votes = r map { _ count })
    }

    def cast(people: Map[String, Person])(jobs: List[Job]) =
        jobs filter { castMember contains _.category } sortBy { _ order } map { j =>
            Role(people get { j person } map { _ name }, Some(j.characters head))
        }

}
