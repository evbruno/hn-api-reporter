# Hacker News API reporter

Uses the hackernews API to report user comments for the top stories

ref: https://github.com/HackerNews/API

## Impl

This version does a simple blocking request for each one of the "kids", builds 
the Story tree in memory, and prints it out the report.

## Running
 
`sbt run`

The default values for `topStories` is `30`, and for `topCommenter` is `2`, the
main app accept them both, in that order:

`sbt "run 10 3" ` 
