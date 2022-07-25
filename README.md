# Hacker News API reporter

Uses the hackernews API to report user comments for the top stories

ref: https://github.com/HackerNews/API

## Implementation details

- The [service](./src/main/scala/br/etc/bruno/hn/services/HackerNewsAPI.scala) that collects data from the API is a plain http client doing synchronous/blocking calls, this service is being called inside Actors
- Some Actors were modeled with [FSM](https://doc.akka.io/docs/akka/current/typed/fsm.html) o handle distinct messages / behaviors
- The output is a _plain_ `println` 

## Running
 
`sbt run`

The default values for `topStories` is `30`, and for `topCommenter` is `5`, the
main app accept them both, in that order:

`sbt "run 10 3" ` 

Also available a `Dockerfile`, we can build and run with: `docker build -t hn-api . && docker run --rm hn-api`

## Tests

Basic Actor behavior specs are available, and it can be run with `sbt test`

## Output

```
[info] ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
[info] |Story                                                                         |#1 Top Commenter                      |#2 Top Commenter                      |#3 Top Commenter                 |
[info] ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
[info] |The Ad Hominem Fallacy Fallacy                                                |badrabbit (1 for story - 1 total)     |lfpeb8b45ez (1 for story - 1 total)   |Maursault (1 for story - 1 total)|
[info] |Ask HN: How to keep water-obsessed autistic child from wasting water?         |bryanrasmussen (4 for story - 5 total)|xupybd (3 for story - 3 total)        |llamaLord (2 for story - 2 total)|
[info] |Soon, life for 40M people who depend on the Colorado River will change        |jacquesm (8 for story - 8 total)      |hinkley (5 for story - 5 total)       |czbond (5 for story - 5 total)   |
[info] |Lizardman's Constant Is 4%                                                    |< no data >                           |< no data >                           |< no data >                      |
[info] |Turn-by-turntables: How drivers got from A to B in the early 1900s (2020)     |dredmorbius (2 for story - 2 total)   |frosted-flakes (1 for story - 1 total)|Theodores (1 for story - 1 total)|
[info] |The inventor of ibuprofen tested the drug on his own hangover                 |rejectfinite (6 for story - 6 total)  |freeflight (5 for story - 5 total)    |Mikeb85 (3 for story - 3 total)  |
[info] |Show HN: SkillPress – Learn JavaScript via spaced repetition and active recall|delabroj (10 for story - 10 total)    |pessimizer (4 for story - 5 total)    |revskill (3 for story - 3 total) |
[info] |I'm running a D&D campaign for my daughter and a friend over Zoom             |toomuchtodo (1 for story - 1 total)   |< no data >                           |< no data >                      |
[info] |In Search of the Continuous Monument (2009)                                   |< no data >                           |< no data >                           |< no data >                      |
[info] |Show HN: I built the social media management platform you'll love             |inspired_prgmr (3 for story - 3 total)|tr1ll10nb1ll (2 for story - 2 total)  |beardyw (2 for story - 2 total)  |
[info] ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
```