# Todo Websockets

[![Build Status](https://travis-ci.org/divanvisagie/todo-websockets.svg?branch=master)](https://travis-ci.org/divanvisagie/todo-websockets)

### Running

First build the polymer application, to do this you will need to install `polymer-cli`

```sh
cd web_client
bower install 
polymer build
```

Next run the scala application via sbt in the root directory of the project

```sh
sbt run
```

Step 3 is to run the mongo server, you can run your own or just spin up
the provided docker compose file with:

```sh
docker-compose up
```


Browse to [localhost:5000](http://localhost:5000/)

