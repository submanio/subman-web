# [subman.io](http://subman.io/) [![Build Status](https://travis-ci.org/submanio/subman-web.svg)](https://travis-ci.org/submanio/subman-web)

Service for fast subtitle searching.

[Chrome extension sources.](https://github.com/subman/subman-chrome)

## Api

For search send GET request like:

    http://subman.io/api/search/?query=file-name
    
Available params:

* `lang` &mdash; language, by default used `english`;
* `source` &mdash; source id, by default used `-1` (equals `all`);
* `limit` &mdash; limit of result, by default used `100`;
* `offset` &mdash; result offset, by default used `0`.

For bulk search send POST request to `http://subman.io/api/bulk-search/` with transit-encoded body with:

* `queries` &mdash; list of queries to search;
* `source` &mdash; source id, by default used `-1` (equals `all`);
* `limit` &mdash; limit of result, by default used `100`;
* `offset` &mdash; result offset, by default used `0`.
    
All languages with subtitles count available in:

    http://subman.io/api/list-languages/

All sources with names available in:

    http://subman.io/api/list-sources/

You can get total subtitles count in:

    http://subman.io/api/count/

For decoding api response you should use [transit](https://github.com/cognitect/transit-format).

## Installation

First you need to install lein, bower, mongodb and elasticsearch.

Then install deps:

```bash
lein deps
lein bower install
```

Prepare assets:

```bash
lein cljsbuild once dev
lein cljx
```

And run with:

```bash
lein ring server
```

For building jar run:

```bash
lein ring uberjar
```

For running server side tests run:

```bash
lein test
```

For client side test install phantomjs and run:

```bash
lein cljsbuild test
```

## Deploy

For testing local changes you need to build docker image:

```bash
docker build -t submanio/subman-web .
```
