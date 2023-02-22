# Task Support

## Rationale

This is an effort to formalize the way we implement job 
processing, especially to implement asynchronous connector
actions.

When implementing such action, the recomended way is to :
* have a service where the representation of the async load
is created (we now call this load representation the task)
* the service is responsible for posting a job that will 
asynchronously handle the load
* the job is responsible, while processing the load, for 
maintaining the task state by calling the service
* the service is responsible for calling back the task 
callee when the task state changes

The task support framework aims at easing :
* defining tasks endpoint in APIs
* implementing the task endpoint
* implementing task job processor

## Task API

## Task endpoint

## Task processing

