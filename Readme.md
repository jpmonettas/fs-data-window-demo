# FlowStorm DataWindow demo

## Intro

This repo is meant to demo some of the features of FlowStorm DataWindows.

It contains a namespace with code that you can progressively evaluate to try the new FlowStorm DataWindows capabilities.

We will be starting the FlowStorm UI to try the examples but nothing here will require instrumentation of any kind.
You can use FlowStorm DataWindows like you use any other data exploration tool like portal, morse, reveal, etc.

If you are a FlowStorm user, DataWindows are replacing the old value inspector, so they will also be available everywhere in the FlowStorm UI.

The goals for the new DataWindows are :

* Provide a way to navigate nested structures in lazy way
* Support lazy/infinite sequences navigation
* Support multiple visualizations for each value
* Provide tools for the user to add custom visualizations on the fly
* Support clojure.datafy navigation out of the box
* Provide mechanisms for realtime data visualization

## How to use it

* clone this repo
* open `scr/data_windows_demo.clj` with your favourite editor/IDE
* start a repl
* follow the comments
