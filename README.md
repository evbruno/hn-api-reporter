# Hacker News API reporter

Uses the hackernews API to report user comments for the top stories

ref: https://github.com/HackerNews/API

## Impl

This version does a simple blocking request for each one of the "kids", builds the Story tree in memory, and prints it out the report.

## Running

`sbt run`
