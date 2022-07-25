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
